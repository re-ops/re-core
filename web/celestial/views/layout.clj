(ns celestial.views.layout
  (:use noir.request)
  (:require 
    [selmer.template-parser :refer [preprocess-template]]
    [selmer.util :refer (resource-path)] 
    [selmer.parser :refer (parse render-template) ]))

(def template-path "celestial/views/templates/")


(defn render [template & [params]]
  #_(parser/render-file (str template-path template)
    (assoc params :context (:context *request*)))
  (render-template 
    (parse (java.io.StringReader. (preprocess-template (str template-path template))) {})
       (assoc params :context (:context *request*))))

