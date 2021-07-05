(ns re-mote.zero.pipeline
  "Base ns for zeromq pipeline support"
  (:require
   [re-mote.zero.callback :refer (register-callback)]
   [clojure.set :refer (rename-keys)]
   [re-mote.spec :as re-spec :refer (valid?)]
   [taoensso.timbre :refer  (refer-timbre)]
   [com.rpl.specter :refer (transform MAP-VALS ALL VAL)]
   [re-mote.zero.management :refer (refer-zero-manage)]
   [re-mote.zero.results :refer (refer-zero-results)]
   [re-mote.zero.functions :refer (call schedule)]
   [re-mote.zero.cycle :refer (ctx)]))

(refer-timbre)
(refer-zero-manage)
(refer-zero-results)

(defn non-reachable
  "Adding non reachable hosts"
  [{:keys [hosts]} up uuid]
  (into {}
        (map
         (fn [h] [h {:code -1 :host h :error {:out "host re-gent not connected"} :uuid uuid}])
         (filter (comp not (partial contains? up)) hosts))))

(defn add-error [errors]
  (transform [MAP-VALS ALL] (fn [m] (rename-keys m {:result :error})) errors))

(defn into-results
  "Create the results map combining results with down hosts"
  [hs hosts uuid results]
  {:post [(valid? ::re-spec/operation-result %)]}
  (let [down (non-reachable hs hosts uuid)
        grouped (group-by :code (vals (merge results down)))
        success (or (grouped 0) [])
        failure (add-error (dissoc grouped 0))]
    {:hosts (keys hosts) :success success :failure failure}))

(defn run-hosts
  "Run function f with provided args on all hosts using Re-gent and collect results:

     ; Run sync and timeout after 10 second if not all results are back
     (run-hosts (hosts (matching (*1))  :hostname) re-cog.facts.security/cpu-vulns [] [10 :second])

    ; Run async and invoke callback when all results are back or timeout has reached
     (run-hosts (hosts (matching (*1))  :hostname) re-cog.facts.security/cpu-vulns [] [10 :second] (fn [timeout? {:keys [success failure]] ...))
  "
  ([hs f args]
   (run-hosts hs f args [10 :second]))
  ([hs f args timeout]
   {:post [(valid? ::re-spec/operation-result %)]}
   (let [hosts (into-zmq-hosts hs)
         uuid (call f args hosts)]
     (into-results hs hosts uuid (collect (keys hosts) uuid timeout))))
  ([hs f args timeout callback]
   (let [hosts (into-zmq-hosts hs)
         uuid (call f args hosts)]
     (register-callback hosts uuid timeout callback))))

(defn schedule-hosts
  "Schedule a function f with provided args on all hosts using Re-gent and confirm registration:

     ; setup a scheduled function with key :cpu-vuln that runs every 20 seconds
     (schedule-hosts (hosts (matching (*1))  :hostname) re-cog.facts.security/cpu-vulns [] [:cpu-vulns 20])
  "
  [hs f args spec]
  {:post [(valid? ::re-spec/operation-result %)]}
  (let [hosts (into-zmq-hosts hs)
        uuid (schedule f args spec hosts)]
    (into-results hs hosts uuid (collect (keys hosts) uuid [10 :second]))))

(defn refer-zero-pipe []
  (require '[re-mote.zero.pipeline :as zpipe :refer (run-hosts schedule-hosts)]))

(comment
  (send- (send-socket @ctx) {:address 1234 :content {:request :execute}}))
