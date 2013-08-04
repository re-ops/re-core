(ns celestial.views.layout
  (:use noir.request)
  (:require [selmer.parser :refer (render-file)]))

(def template-path "celestial/views/templates/")

(defn render [template & [params]]
  (render-file (str template-path template)
    (assoc params :context (:context *request*))))

