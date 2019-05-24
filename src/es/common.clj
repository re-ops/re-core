(ns es.common
  "Common ES"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-share.es.common :as es]))

(refer-timbre)

(def ^:const types {:jobs {:properties {:env {:type "keyword"}
                                        :status {:type "text"}
                                        :queue {:type "text"}
                                        :start {:type "long"}
                                        :end {:type "long"}}}
                    :system {:properties {:machine {:type :object
                                                    :properties {:user {:type "keyword"}
                                                                 :domain {:type "keyword"}
                                                                 :os {:type "keyword"}
                                                                 :hostname {:type "keyword"}
                                                                 :cpu {:type "long"}
                                                                 :ram {:type "float"}}}
                                          :type {:type "keyword"}}}
                    :type {:properties {:description {:type "text"}}}})

(defn index
  "get re-core index"
  [type]
  (es/index :re-core type))

(defn initialize
  "Create re-core Elasticsearch mappings and types"
  []
  (es/initialize :re-core types false))
