(ns celestial.model 
  "Model manipulation ns"
  )

(defmulti translate
  "Converts general model to specific virtualization model" 
  (fn [spec] (first (remove #{:machine :type} (keys spec)))))


(defmulti construct 
  "Creates a Virtualized instance model from input edn" 
  (fn [spec]
    (first (remove #{:machine :type} (keys spec))))
  )

