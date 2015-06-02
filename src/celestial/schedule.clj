(ns celestial.schedule
  "Scheduled tasks"
 (:require 
   [chime :refer [chime-ch]]
   [clj-time.core :as t]
   [clojure.core.async :as a :refer [<! go-loop]])
  )  

(defn schedule [times]
  (let [chimes (chime-ch times)]
    (a/<!! 
      (go-loop []
       (when-let [msg (<! chimes)]
         (prn "Chiming at:" msg)
         (recur))))))

#_(schedule (periodic-seq (t/now) (-> 5 t/minutes)))
