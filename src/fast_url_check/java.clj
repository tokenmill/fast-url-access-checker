(ns fast-url-check.java
  (:gen-class
    :name crawl.tools.URLCheck
    :methods [^{:static true} [checkAccess [String] java.util.Map]
              ^{:static true} [checkAccess [String java.util.Map] java.util.Map]
              ^{:static true} [checkAccessBulk [java.util.Collection] java.util.Collection]
              ^{:static true} [checkAccessBulk [java.util.Collection java.util.Map] java.util.Collection]])
  (:require [fast-url-check.core :refer :all]
            [clojure.walk :refer [keywordize-keys stringify-keys]]))

(defn -checkAccess
  ([url]
   (-checkAccess url nil))
  ([url opts]
   (-> (check-access url :opts (keywordize-keys opts))
       (update :status-type name)
       (stringify-keys))))

(defn -checkAccessBulk
  ([urls]
   (-checkAccessBulk urls nil))
  ([urls opts]
   (map #(-> % (update :status-type name) (stringify-keys))
        (check-access-bulk urls :opts (keywordize-keys opts)))))
