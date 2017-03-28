(ns re-core.persistency.quotas
 (:require  
    [subs.core :refer (validate! when-not-nil validation every-kv)]
    [puny.core :refer (entity)]
    [re-core.persistency.users :refer [user-exists?]]
    [re-core.model :refer (figure-virt)] 
    [slingshot.slingshot :refer  [throw+]]
    [clojure.core.strint :refer (<<)]))

(defn used-key [{:keys [env] :as spec}]
  [:quotas env (figure-virt spec) :used :count])

(entity quota :id username)

(validation :user-exists (when-not-nil user-exists? "No matching user found"))
 

(def quota-v
  {:username #{:required :user-exists} 
   :quotas {
      :subs/ANY { 
        :subs/ANY {:limits { :count #{:required :Integer}} :used  { :count #{:required :Integer}}
    }}}})

(defn validate-quota [{:keys [quotas] :as q}]
  (validate! q quota-v :error ::non-valid-quota))
 
(defn quota-assert
  [{:keys [owner env] :as spec}]
  (let [hyp (figure-virt spec) 
        q (or (get-quota owner) {})
       {:keys [limits used]} (get-in q [:quotas env hyp])]
    (when (and (not (empty? q)) (= (:count used) (:count limits)))
      (throw+ {:type ::quota-limit-reached} (<< "Quota limit ~{limits} on ~{hyp} for ~{owner} was reached")))))

(defn quota-change [{:keys [owner] :as spec} f]
    (when (quota-exists? owner)
      (update-quota 
        (update-in (get-quota owner) (used-key spec) f))))

(defn increase-use 
  "increases user quota use"
  [spec]
  (quota-change spec inc))

(defn decrease-use 
  "decreases usage"
  [{:keys [owner] :as spec}]
  (when (get-in (get-quota owner) (used-key spec))
    (quota-change spec dec)))

(defmacro with-quota [spec & body]
  `(do 
     (quota-assert ~spec)
      ~@body
     (increase-use ~spec)))
 
