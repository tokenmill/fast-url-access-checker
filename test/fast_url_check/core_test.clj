(ns fast-url-check.core-test
  (:require [clojure.test :refer :all]
            [fast-url-check.core :refer :all]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(deftest url-validator-test
  (testing "Access check"
    (are [url result] (= result (dissoc (check-access url) :response-time))
      nil             nil
      "tokenmill.lt"  {:url         "http://www.tokenmill.lt/"
                       :status      200
                       :status-type :accessible
                       :seed        "tokenmill.lt"}
      "tokenmill-.lt" {:url         "tokenmill-.lt"
                       :status      999
                       :status-type :error
                       :exception   "host is null"
                       :seed        "tokenmill-.lt"}))
  (testing "Access check bulk"
    (is (= [] (check-access-bulk nil)))
    (is (= [{:url "http://www.tokenmill.lt/", :seed "tokenmill.lt", :status-type :accessible, :status 200}
            {:url "https://www.15min.lt/", :seed "15min.lt", :status-type :accessible, :status 200}
            {:url "https://www.google.lt/", :seed "google.lt", :status-type :accessible, :status 200}]
           (map #(dissoc % :response-time) (check-access-bulk ["tokenmill.lt" "15min.lt" "google.lt"])))))
  (testing "URL generation"
    (let [result-1 ["https://www.15min.lt/" "https://15min.lt/" "http://www.15min.lt/" "http://15min.lt/"]]
      (are [url result] (= (generate-candidates url) result)
        "https://www.15min.lt" result-1
        "https://15min.lt"     result-1
        "http://www.15min.lt"  result-1
        "http://15min.lt"      result-1
        ":www.15min.lt"        [":www.15min.lt"]
        "www.  15min.lt"       ["www.  15min.lt"])))
  (testing "Error message parsing"
    (are [url message result] (= result (remove-url-in-error-message message url))
      "https://www.pcbasics.biz/"
      "www.pcbasics.biz: Name or service not known"
      "Name or service not known"

      "http://www.1up-it.-com/"
      "host is null: http://www.1up-it.-com/"
      "host is null"

      "﻿http://www.0-downtime.com"
      "Illegal character in scheme name at index 0: ﻿http://www.0-downtime.com"
      "Illegal character in scheme name at index 0"

      "http://www.advens.fr home.asp"
      "Illegal character in authority at index 7: http://www.advens.fr home.asp"
      "Illegal character in authority at index 7")))

(deftest candidate-generation
  (is (= ["https://www.tokenmill.lt/"
          "https://tokenmill.lt/"
          "http://www.tokenmill.lt/"
          "http://tokenmill.lt/"]
         (generate-candidates "tokenmill.lt")))
  (is (= ["https://www.tokenmill.lt/"
          "https://tokenmill.lt/"
          "http://www.tokenmill.lt/"
          "http://tokenmill.lt/"]
         (generate-candidates "https://tokenmill.lt/"))))

(deftest resolve-redirect-url-test
  (is (= "" (resolve-redirect-url "" "")))
  (is (= "http://dsempire.org/kNeQm/" (resolve-redirect-url "http://dsempire.org" "/kNeQm/")))
  (is (= "https://dsempire.org/" (resolve-redirect-url "http://dsempire.org" "https://dsempire.org/"))))

(deftest ^:integration url-validator-benchmark
  (with-open [rdr (io/reader (io/file "test/resources/bulk-test.txt"))]
    (let [urls (take 100 (shuffle (line-seq rdr)))
          start-time (System/currentTimeMillis)]
      (doseq [validated-url (check-access-bulk urls)]
        (log/infof "%s - %s" (:url validated-url) (:status validated-url)))

      (let [total-time-seconds (float (/ (- (System/currentTimeMillis) start-time) 1000))
            average-time-seconds (/ total-time-seconds (count urls))]
        (is (> 0.3 average-time-seconds))

        (log/infof "Number of URLs tested: %s" (count urls))
        (log/infof "Total time: %s seconds" total-time-seconds)
        (log/infof "Average time: %s seconds" average-time-seconds)))))
