(ns supernal.core
  "A remote task execution framework (similar to fabric and capistrano) with follwing features:
  * Basic capistrano like functionality
  * A central server for running tasks
  * Clear seperation between tasks and execution/dependency order
  * Dynamic role and host lookup for tasks invocation targets
  * First class (java) package deployment lifecycle
  * can be used as a library and as a standalone tool  
  * Zeromq agent for improved perforemance over basic ssh
  "
  (:require 
       [clojure.walk :as walk]
       [supernal.sshj :as sshj]) 
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.topsort :only (kahn-sort)] 
    (clojure.pprint)))


(defn gen-ns [ns*]
  (symbol (str "supernal.user." ns*)))

(defn apply-remote 
  "Applies call to partial copy and run functions under tasks,
  (copy foo bar) is transformed into ((copy from to) remote)"
  [body]
  (let [shim #{'run 'copy}]
    (walk/postwalk #(if (and (seq? %) (shim (first %))) (list % 'remote) %) body)))

(defn task 
  "Maps a task defition into a functions named name* under ns* namesapce" 
  [ns* name* body]
  (list 'intern (list symbol ns*) (list symbol name*) (concat '(fn [args remote]) (apply-remote body))))

(defmacro ns- 
  "Tasks ns macro, a group of tasks is associated with matching functions under the supernal.user ns"
  [ns* & tasks]
  `(do 
     (create-ns '~(gen-ns ns*))
     ~@(map 
         (fn [[_ name* & body]]
           (task (str (gen-ns ns*)) (str name*) body)) tasks))) 

(defn gen-lifecycle [ns*]
  (symbol (str ns* "-lifecycle")))

(defn resolve- [sym]
  (let [[pre k] (.split (str sym) "/")]
    (if-let [res (ns-resolve (symbol (str "supernal.user." pre)) (symbol k))]
      res
      (throw (Exception. (<< "No symbol ~{k} found in ns ~{pre}"))))))

(defmacro lifecycle 
  "Generates a topological sort from a lifecycle plan"
  [name* plan]
  `(def ~name*
     (with-meta
       (kahn-sort 
         (reduce (fn [r# [k# v#]] 
                   (assoc r# (resolve- k#) 
                          (into #{} (map #(resolve- %) v#)))) {} '~plan)) {:plan '~plan})))

(defn run-cycle [cycle* args remote]
  (doseq [t cycle*]
    (t args remote)))

(defn run-id [args]
  (assoc args :run-id (java.util.UUID/randomUUID)))

(defmacro execute [name* args role]
  "Executes a lifecycle defintion on a given role"
  `(doseq [remote# (get-in @~'env- [:roles ~role])] 
     (future (run-cycle ~name* (run-id ~args) remote#))))

(defmacro execute-task 
  "Executes a single task on a given role"
  [name* args role]
  `(doseq [remote# (get-in @~'env- [:roles ~role])] 
     (future ((resolve- '~name*) (run-id ~args) remote#))))

(defmacro env 
  "A hash of running enviroment info and roles"
  [hash*]
  `(intern *ns* (symbol  "env-") (atom ~hash*)))

(defn role->hosts 
  "A hash based role to hosts resolver"
  [role] 
  )

(defn run
  "Running a given cmd on a remote system" 
  [cmd]
  (fn [remote] 
    (let [cmd* (if (-> remote :sudo) (str "sudo " cmd) cmd)]
      (sshj/execute cmd* remote))))

(defn copy
  "Copies src uri (either http/file/git) into a remote destination path" 
  [src dst]
  (partial sshj/copy src dst))


