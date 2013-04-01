(ns celestial.test.supernal
   (:use clojure.test 
     [supernal.core :only (apply-remote)]))

(deftest nested-remote-apply 
 (is (= (apply-remote '(let [1 2] (copy 1 2))) '(let [1 2] ((copy 1 2) remote)))))
