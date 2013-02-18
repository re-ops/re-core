(ns celestial.persistency
 )

(def hosts 
  {:redis-test.local {:classes {:redis {:append true}}}
   :redis-test.home.net {:classes {:redis {:append true}}}
   :redis {:classes {:redis {:append true}}}
   })

(defn profile [host]
  (hosts host))
