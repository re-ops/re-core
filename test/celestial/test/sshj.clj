(ns celestial.test.sshj
  (:use celestial.sshj clojure.test)
 )

(def tasks
  { :download {
      :wget "wget,,," :tar "tar -xvzf"
     }
     :puppet ^{:depends #{:download :yml-up}} {
        :cd "cd /tmp/~{module}" :run "./scripts/run"
     } 
     :yml-up ^{:depends #{:download}}{
        :cd "cd /tmp/~{module}" :run "./scripts/run"
     }
     :clean ^:last {
      :rm "rm -rf /tmp/~{module}"
     }
   }
  )

(deftest deps
  (is (= (sorted-deps tasks) [:download :yml-up :puppet :clean])))
