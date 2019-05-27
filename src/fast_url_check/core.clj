(ns fast-url-check.core
  (:require [clojure.tools.logging :as log]
            [org.httpkit.client :as http]
            [clojure.string :as s]
            [clojurewerkz.urly.core :as u]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.data.csv :as csv])
  (:import (org.httpkit.client TimeoutException)
           (java.time Instant)))

(def default-opts
  {:keepalive        -1
   :socket-timeout   5000
   :conn-timeout     5000
   :timeout          5000
   :insecure?        true
   :follow-redirects false
   :user-agent       "Mozilla/5.0 (X11\\; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36"
   :headers          {"accept-language" "en-US,en;q=0.8,lt;q=0.6"
                      "accept"          "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"}})

(defn generate-candidates
  "Given URL a series of candidate url modifications are produced.
  For example starting with 'example.org' it will mutate this initial host string
  with different protocols and optional path elements.

  Host mutations are defined in terms of protocol changes (https, http)
  and host formatting with optional 'www' part.

  Ordering of mutations is important for generated URLs will be checked
  in mutation order. Thus the right order saves time on network operations. "
  [url]
  (try
    (let [url-obj (u/url-like (s/trim url))
          host (-> url-obj (u/host-of) (s/replace #"(?i)^www\." ""))
          url-variation-templates [["https" "www.%s"]
                                   ["https" "%s"]
                                   ["http" "www.%s"]
                                   ["http" "%s"]]]
      (map (fn [[protocol host-template]]
             (-> url-obj (.mutateProtocol protocol) (.mutateHost (format host-template host)) (str)))
           url-variation-templates))
    (catch Exception e
      (log/debugf "Failed to generate candidates for %s with error %s" url (.getMessage e))
      [url])))

(defn remove-url-in-error-message [message url]
  (let [url-obj (u/url-like url)
        host (u/host-of (or url-obj ""))]
    (reduce #(s/replace %1 %2 "") message [url (or host "") #"^: " #": $"])))

(defn resolve-redirect-url [url redirect-url]
  (if (u/relative? redirect-url)
    (u/resolve url redirect-url)
    redirect-url))

(defn send-request
  "Sends http request to `url` and returns a map with response information as well as `seed`.
  Default options for 'http-kit' may be altered by providing option map for keyword argument `:opts`.

  Responses are categorized based on their status:
  2**: accessible
  3**: redirect
  4**: client-error
  5**: server-error
  Additional types: timeout, error, other, too-many-retries."
  [url seed & {:keys [opts]}]
  (let [start-time (System/currentTimeMillis)]
    (http/request
      (merge default-opts opts {:url url :method :head})
      (fn [{:keys [status headers error]}]
        (let [redirect-url (get headers :location "")
              status-type (some-> status (str) (first) (Character/getNumericValue))
              exception (some-> error (.getMessage) (remove-url-in-error-message url))]
          (merge {:url           url
                  :seed          seed
                  :status        status
                  :response-time (- (System/currentTimeMillis) start-time)}
                 (cond
                   (= 2 status-type) {:status-type :accessible}
                   (= 3 status-type) {:url (resolve-redirect-url url redirect-url) :status-type :redirect}
                   (= 4 status-type) {:status-type :client-error}
                   (= 5 status-type) {:status-type :server-error}
                   (instance? TimeoutException error) {:status 408 :status-type :timeout}
                   (some? error) {:status 999 :status-type :error :exception exception}
                   :else {:status-type :other})))))))

(defn send-request-with-retry [url seed & {:keys [opts]}]
  (loop [retry-count 0]
    (let [{:keys [response exception]}
          (try
            {:response (send-request url seed :opts opts)}
            (catch Exception e
              (log/debugf "Failed to send request to %s with error %s" url (.getMessage e))
              {:exception (.getMessage e)}))]
      (cond
        (some? response) response
        (< 2 retry-count) (ref {:url url :seed seed :status-type :too-many-retries :exception exception})
        :else (recur (inc retry-count))))))

(defn pick-candidate
  "Selects most fit candidate based on it's status type given collection of response maps.
  If there are multiple candidates of the same status type, first one is returned."
  [responses]
  (let [status-types (group-by :status-type responses)]
    (loop [status [:accessible :redirect :other :timeout :client-error :server-error :error :too-many-retries]]
      (when-let [[x & xs] status]
        (if (contains? status-types x)
          (first (get status-types x))
          (recur xs))))))

(defn check-access [url & {:keys [opts]}]
  (some->> url
           (generate-candidates)
           (map #(send-request-with-retry % url :opts opts))
           (doall)
           (map deref)
           (pick-candidate)))

(defn check-access-bulk [urls & {:keys [opts]}]
  (pmap #(check-access % :opts opts) urls))

(defn -main
  "Given path to file containing list of URLs as `input-path`,
  prints CSV lines with validated URLs to standard output.
  Additional http-kit options may be provided as JSON encoded map."
  [& [input-path opts]]
  (with-open [rdr (io/reader (io/file input-path))]
    (csv/write-csv *out* [["timestamp" "seed" "url" "status" "status-type" "response-time" "exception"]])
    (doseq [{:keys [url seed status status-type response-time exception]}
            (check-access-bulk (line-seq rdr) :opts (json/decode opts true))]
      (csv/write-csv *out* [[(Instant/now) seed url status (name status-type) response-time exception]])
      (flush))))
