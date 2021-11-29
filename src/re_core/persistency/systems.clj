(ns re-core.persistency.systems
  (:require
   [re-cog.facts.datalog :refer (flatten-keys join-keys fact-pairs)]
   [xtdb.api :as xt]
   [re-share.core :refer (gen-uuid)]
   [re-core.model :as model]
   [taoensso.timbre :refer (refer-timbre)]
   [re-core.persistency.xtdb :refer [node]]))

(refer-timbre)

(defn unflatten [m]
  (reduce
   (fn [m [k v]]
     (assoc-in m (mapv keyword (clojure.string/split (subs (str k) 1) #"\/")) v)) {} m))

(defn flatten- [m]
  (into {} (map first (map join-keys (flatten-keys m)))))

(defn get-system-by-host
  "Search for a system using its hostname"
  [host]
  (map (comp unflatten first)
       (xt/q (xt/db node)
             '{:find [(pull ?system [*])]
               :where [[?system :machine/hostname host]]
               :in [host]} host)))

(defn missing-systems
  "Filter systems that aren't persisted already"
  [systems]
  (filter
   (fn [{:keys [machine]}] (empty? (get-system-by-host (machine :hostname)))) systems))

(defn put
  "Update a system"
  [id system]
  (xt/await-tx node (xt/submit-tx node [[::xt/put (assoc (flatten- system) :xt/id id)]])) nil)

(defn create
  "create a system returning its id"
  ([system]
   (create system (gen-uuid)))
  ([system id]
   (xt/await-tx node (xt/submit-tx node [[::xt/put (assoc (flatten- system) :xt/id id)]])) id))

(defn delete
  "Delete a system"
  [id]
  (xt/await-tx node (xt/submit-tx node [[::xt/evict id]])) nil)

(defn get
  "Grabs a system by an id, return nil if missing"
  [id]
  (unflatten (xt/pull (xt/db node) '[*] id)))

(defn exists?
  [id]
  (not (empty? (get id))))

(defn get!
  "Grabs a system by an id, fail if not found"
  [id]
  (if-let [result (get id)]
    result
    (throw (ex-info "Missing system" {:id id}))))

(defn partial
  "Partial update of a system"
  [id part]
  (let [system (get id)]
    (put id (merge-with merge system part))))

(defn clone
  "clones an existing system"
  [id {:keys [hostname] :as spec}]
  (put
   (-> (get id)
       (assoc-in [:machine :hostname] hostname)
       (model/clone spec))))

(defn system-val
  "grabbing instance id of spec"
  [spec ks]
  (get-in (get (spec :system-id)) ks))

(defn all
  "return all existing systems"
  []
  (map (fn [[m]] [(:xt/id m) (unflatten (dissoc m :xt/id))])
       (xt/q (xt/db node) '{:find [(pull ?system [*])] :where [[?system :type _]]})))

(defn update-ip
  "updates public ip address in the machine persisted data"
  [system-id ip]
  (when (exists? system-id)
    (partial system-id {:machine {:ip ip}})))

(comment
  (def redis-lxc
    {:machine {:hostname "red1" :user "root" :domain "local"
               :os :ubuntu-18.04.2 :cpu 4 :ram 1}
     :lxc {:node :localhost}
     :type :redis})

  (def redis-lxc-missing
    {:machine {:hostname "foo" :user "root" :domain "local"
               :os :ubuntu-18.04.2 :cpu 4 :ram 1}
     :lxc {:node :localhost}
     :type :redis})
  (clone "123" {:hostname "foo"})
  (missing-systems [redis-lxc redis-lxc-missing])
  (partial "123" {:machine {:ip 123}})
  (put "123" redis-lxc)
  (system-val {:system-id "123"} [:machine :hostname])
  (get "123")
  (delete "123")
  (exists? "123")
  (update-ip "123" "192.168.5.1")
  (create redis-lxc "123")
  (put "123" redis-lxc))
