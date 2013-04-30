(ns puny.core
  "A tiny redis based ORM,
   Caveat: crud+index operations are not atomic, introducing lua procedures will fix that."
  (:use
    [slingshot.slingshot :only  [throw+ try+]]
    [celestial.redis :only (wcar hsetall*)]
    [clojure.core.strint :only (<<)])
  (:require 
    [taoensso.carmine :as car])
 )

(defmacro <<< 
  "String interpulation into a symbol"
  [s] `(symbol (<< ~s)))

(defmacro <<k 
  "String interpulation into a keyword"
  [s] `(keyword (<< ~s)))

(defn fn-ids [name*]
  {:id-fn (<<< "~{name*}-id") :exists-fn (<<< "~{name*}-exists?")
   :add-fn (<<< "add-~{name*}") :update-fn (<<< "update-~{name*}")
   :validate-fn (<<< "validate-~{name*}") :gen-fn (<<< "gen-~{name*}-id") 
   :delete-fn (<<< "delete-~{name*}") :get-fn (<<< "get-~{name*}")
   :partial-fn (<<< "partial-~{name*}")})

(defn id-modifiers [name* opts]
  (if-let [id-prop (opts :id)]
     {:up-args (vector {:keys [id-prop] :as 'v}) :up-id id-prop :add-k-fn (list 'v (keyword id-prop))}
     {:up-args ['id 'v] :up-id 'id :add-k-fn (list (:gen-fn (fn-ids name*)))}))

(defmacro defgen 
  "An id generator" 
  [name*]
  `(defn ~(<<< "gen-~{name*}") []
    (wcar (car/incr ~(<< "~{name*}:ids")))))

(defn indices-fn-ids [name*]
  {:index-add (<<< "index-~{name*}") :index-del (<<< "clear-~{name*}-indices")
   :index-get (<<< "get-~{name*}-index") :reindex (<<< "reindex-~{name*}") })

(defmacro index-fns
  "Create index functions (enabled if there are indices defined)."
  [name* {:keys [indices]}]
  (let [{:keys [index-add index-del index-get reindex]} (indices-fn-ids name*)
        indices-ks (into [] (map keyword indices))] 
    `(do 
       (defn ~index-add [~'id ~'v]
         (doseq [i# ~indices-ks]
           (wcar (car/sadd (str '~name* i# (get ~'v i#)) ~'id ))))

       (defn ~index-get [~'k ~'v]
         (wcar (car/smembers (str '~name* ~'k ~'v))))

       (defn ~index-del [~'id ~'v]
         (doseq [i# ~indices-ks]
           (wcar (car/srem (str '~name* i# (get ~'v i#)) ~'id))))

        (defn ~reindex [~'id ~'old ~'new]
          (~index-del ~'id ~'old) 
          (~index-add ~'id ~'new)))))

(defmacro write-fns 
  "Creates the add/update functions both take into account if id is generated of provided"
  [name* opts]
  (let [{:keys [id-fn exists-fn validate-fn add-fn update-fn gen-fn get-fn partial-fn]} (fn-ids name*)
        missing (<<k ":~{*ns*}/missing-~{name*}") 
        {:keys [up-args up-id add-k-fn]} (id-modifiers name* (apply hash-map opts))
        {:keys [index-add index-del reindex]} (indices-fn-ids name*) ]
    `(do 
       (declare ~validate-fn)

       (defn ~gen-fn []
         (wcar (~id-fn (car/incr ~(<< "~{name*}:ids")))))

       (defn ~add-fn [~'v]
         (~validate-fn ~'v)
         (let [id# ~add-k-fn]
           (wcar (hsetall* (~id-fn id#) ~'v)) 
           (~index-add id# ~'v)
           id#))

       (defn ~partial-fn ~up-args
         (when-not (~exists-fn ~up-id)
           (throw+ {:type ~missing ~(keyword name*) ~'v }))
         (let [orig# (wcar (car/hgetall* (~id-fn ~up-id))) updated# (merge-with merge orig# ~'v)]
           (~reindex ~up-id orig# updated#) 
           (wcar (hsetall* (~id-fn ~up-id) updated#))))

       (defn ~update-fn ~up-args
         (~validate-fn ~'v)
         (when-not (~exists-fn ~up-id)
           (throw+ {:type ~missing ~(keyword name*) ~'v }))
         (let [orig# (wcar (car/hgetall* (~id-fn ~up-id))) updated# (merge orig#  ~'v)]
           (~reindex ~up-id orig# updated#) 
           (wcar (hsetall* (~id-fn ~up-id) updated#)))))))

(defmacro entity
  "Generates all the persistency (add/delete/exists etc..) functions for given entity"
  [name* & opts]
  (let [{:keys [id-fn delete-fn get-fn exists-fn]} (fn-ids name*)
        {:keys [index-del]} (indices-fn-ids name*)]
    `(do 
       (defn ~id-fn [~'id] (str '~name* ":" ~'id))

       (defn ~exists-fn [~'id] (not= 0 (wcar (car/exists (~id-fn ~'id)))))

       (index-fns ~name* ~opts)

       (write-fns ~name* ~opts)

       (defn ~get-fn [~'id] (wcar (car/hgetall* (~id-fn ~'id))))

       (defn ~delete-fn [~'id] 
         (~index-del ~'id (~get-fn ~'id)) 
         (wcar (car/del (~id-fn ~'id)))
         ))))

