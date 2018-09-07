(ns es.common
  "Common ES"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-share.es.common :as share :refer (create-index exists?)]))

(refer-timbre)

(def ^:const types {:jobs {:properties {:env {:type "keyword"}
                                        :status {:type "text"}
                                        :queue {:type "text"}
                                        :start {:type "long"}
                                        :end {:type "long"}}}
                    :system {:properties {:owner {:type "text"}
                                          :env {:type "keyword"}
                                          :machine {:properties {:hostname {:type "keyword" :index false}
                                                                 :cpus {:type "integer"}}}
                                          :type {:type "keyword"}}}
                    :type {:properties {:puppet {:properties {:src {:type "text"}
                                                              :tar {:type "text"}}}
                                        :description {:type "text"}}}})

(defn index
  "get re-core index"
  [type]
  (share/index :re-core type))
