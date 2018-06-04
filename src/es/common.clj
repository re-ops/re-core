(ns es.common
  "type mappings index etc.."
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.common :refer (get!)]
   [re-share.es.common :refer (create-index exists?)]))

(refer-timbre)

(def conn-prefix (atom :default))

(defn get-es! [& ks]
  (apply get! :elasticsearch @conn-prefix ks))

(defn prefix-switch
  "Change es prefix"
  [k]
  (reset! conn-prefix k))

(defn index []
  (get-es! :index))

(def ^:const types {:jobs {:properties {:env {:type "keyword"}
                                        :status {:type "text"}
                                        :queue {:type "text"}
                                        :start {:type "long"}
                                        :end {:type "long"}}}
                    :system {:properties {:owner {:type "text"}
                                          :env {:type "keyword"}
                                          :machine {:properties {:hostname {:type "keyword" :index "not_analyzed"}
                                                                 :cpus {:type "integer"}}}
                                          :type {:type "keyword"}}}
                    :type {:properties {:puppet {:properties {:src {:type "text"}
                                                              :tar {:type "text"}}}
                                        :description {:type "text"}
                                        :type {:type "keyword"}}}})

(def ^:const settings {:number_of_shards 1})

(defn initialize
  "Creates systems index and types"
  [index]
  (when-not (exists? index)
    (info "Creating index" index)
    (create-index index {:mappings types})))
