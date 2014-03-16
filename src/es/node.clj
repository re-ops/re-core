(ns es.node
  "An embedded ES node instance"
  (:require 
    [clojurewerkz.elastisch.native :as es]
    [clojurewerkz.elastisch.native.index :as idx]
    [clojurewerkz.elastisch.native.document :as doc]))

(def ES (atom nil))

(defn start-node 
  "launch en embedded ES node" 
  []
  (reset! ES 
          (es/start-local-node
            (es/build-local-node
              {:node.name "celestial" 
               :cluser.name "celestial-cluster"
               :path.logs "/tmp/celestial-es"
               :path.data "/tmp/celestial-es"
               :path.work "/tmp/celestial-es"
               :local true :http.enabled false }))))

(defn stop-node 
  "stops embedded ES node" 
  []
  (.close @ES) 
  (reset! ES nil))

(comment (start-node)
         (stop-node)
         ;; (println @ES)
         (es/connect-to-local-node! @ES)
         (idx/create "myapp2_development" )
         (doc/put "myapp2_development" "person"  "10" {:username "ronen"})
         (doc/get "myapp2_development" "person" "10"))
