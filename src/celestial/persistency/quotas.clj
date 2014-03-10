(ns celestial.persistency.quotas
 (:require  
    [subs.core :refer (validate! when-not-nil validation every-kv)]
    [puny.core :refer (entity)]
    [celestial.persistency :refer [user-exists?]]
    [celestial.model :refer (figure-virt)] 
    [slingshot.slingshot :refer  [throw+]]
    [clojure.core.strint :refer (<<)]))

(defn used-key [{:keys [env] :as spec} k]
  [:quotas env (figure-virt spec) :used :count])

(entity quota :id username)

(validation :user-exists (when-not-nil user-exists? "No matching user found"))
 
(validation :env
   (every-kv {:subs/ANY {:subs/ANY {:limit #{:required :Integer}}}}))

(def quota-v
  {:username #{:required :user-exists} :quotas #{:required :Map :env*}})

(defn validate-quota [{:keys [quotas] :as q}]
  (validate! q quota-v :error ::non-valid-quota)
  (doseq [[k e] quotas]
    (validate! e {
      :limits {:count #{:required :Integer}}
      :used {:count #{:required :Integer}}})))
 
(defn quota-assert
  [{:keys [owner env] :as spec}]
  (let [hyp (figure-virt spec) 
       {:keys [limits used]} (get-in (get-quota owner) [:quotas hyp env])]
    (when (= (:count used) (:count limits))
      (throw+ {:type ::quota-limit-reached} (<< "Quota limit ~{limits} on ~{hyp} for ~{owner} was reached")))))

(defn quota-change [{:keys [owner] :as spec} f]
    (when (quota-exists? owner)
      (update-quota 
        (update-in (get-quota owner) (used-key spec) f))))

(defn increase-use 
  "increases user quota use"
  [id spec]
  (quota-change spec inc))

(defn decrease-use 
  "decreases usage"
  [id {:keys [owner] :as spec}]
  (when-not (empty? (get-in (get-quota owner) (used-key spec)))
    (quota-change spec dec)))

(defmacro with-quota [action spec & body]
  `(do 
     (quota-assert ~spec)
     (let [~'id ~action]
       (increase-use ~'id ~spec)    
       ~@body)))
 
