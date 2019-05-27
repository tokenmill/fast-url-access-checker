(ns fast-url-check.benchmark
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [fast-url-check.core :refer [check-access-bulk]]))

(defn -main [& [input-path]]
  (with-open [rdr (io/reader (io/file (or input-path "test/resources/bulk-test.txt")))]
    (let [urls (line-seq rdr)
          start-time (System/currentTimeMillis)]
      (doseq [validated-url (check-access-bulk urls)]
        (log/infof "%s - %s" (:url validated-url) (:status validated-url)))
      (let [total-time-seconds (float (/ (- (System/currentTimeMillis) start-time) 1000))
            average-time-seconds (/ total-time-seconds (count urls))]
        (log/infof "Number of URLs tested: %s" (count urls))
        (log/infof "Total time: %s seconds" total-time-seconds)
        (log/infof "Average time: %s seconds" average-time-seconds)))))
