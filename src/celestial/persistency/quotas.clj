(ns celestial.persistency.quotas
 (:require  
    [subs.core :refer (validate! when-not-nil validation every-kv)]
    [puny.core :refer (entity)]
    [celestial.persistency :refer [user-exists?]]
    [celestial.model :refer (figure-virt)] 
    [slingshot.slingshot :refer  [throw+]]
    [clojure.core.strint :refer (<<)]))

(defn used-key [spec]
  [:quotas (figure-virt spec) :used])

(entity quota :id username)

(validation :user-exists (when-not-nil user-exists? "No matching user found"))
 
(validation :quota* (every-kv {:limit #{:required :Integer}}))

(def quota-v
  {:username #{:required :user-exists} :quotas #{:required :Map :quota*}})

(defn validate-quota [q]
  (validate! q quota-v :error ::non-valid-quota))
 
(defn quota-assert
  [{:keys [owner] :as spec}]
  (let [hyp (figure-virt spec) {:keys [limit used]} (get-in (get-quota owner) [:quotas hyp])]
    (when (= (count used) limit)
      (throw+ {:type ::quota-limit-reached} (<< "Quota limit ~{limit} on ~{hyp} for ~{owner} was reached")))))

(defn quota-change [id {:keys [owner] :as spec} f]
    (when (quota-exists? owner)
      (update-quota 
        (update-in (get-quota owner) (used-key spec) f id))))

(defn increase-use 
  "increases user quota use"
  [id spec]
  (quota-change id spec (fnil conj #{id})))

(defn decrease-use 
  "decreases usage"
  [id {:keys [owner] :as spec}]
  (when-not (empty? (get-in (get-quota owner) (used-key spec)))
    (quota-change id spec (fn [old id*] (clojure.set/difference old #{id*})))))

(defmacro with-quota [action spec & body]
  `(do 
     (quota-assert ~spec)
     (let [~'id ~action]
       (increase-use ~'id ~spec)    
       ~@body)))
 
