(ns re-mote.repl.re-gent
  "Copy .curve server public key and run agent remotly"
  (:require
   [re-share.core :refer (gen-uuid)]
   [re-cog.scripts.common :refer (validate!)]
   [clojure.java.shell :refer (sh with-sh-dir)]
   [pallet.stevedore :refer (script chained-script)]
   [clojure.core.strint :refer (<<)]
   [re-mote.zero.server :refer (used-port)]
   [re-mote.zero.management :as zm]
   [re-mote.ssh.pipeline :refer (run-hosts)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(defprotocol Regent
  (kill-agent
    [this]
    [this m])
  (unregister-hosts
    [this])
  (start-agent
    [this home]
    [this m home]))

(defn kill-script []
  (script
   ("/usr/bin/systemctl" "--user" "stop" "re-gent.service")))

(defn start-script [port home level]
  (let [bin (<< "~{home}/re-gent") cmd (<< "\"~{bin} ${IP} ~{port} ~{level} &\"")]
    (script
     (set! IP @(pipe ("echo" "$SSH_CLIENT") ("awk" "'{print $1}'")))
     ("chmod" "+x"  ~bin)
     ("nohup" "sh" "-c" ~cmd "&>/dev/null"))))

(defn start-script [port home level]
  (let [bin (<< "~{home}/re-gent") env (<< "~{home}/.re-gent.env")]
    (script
     (set! IP @(pipe ("echo" "$SSH_CLIENT") ("awk" "'{print $1}'")))
     (set! PORT ~port)
     (set! LOG ~level)
     ("rm" ~env "-f")
     ("touch" ~env)
     ("echo" "SERVER=$IP" ">>" ~env)
     ("echo" "PORT=$PORT" ">>" ~env)
     ("echo" "LOG=$LOG" ">>" ~env)
     ("chmod" "+x" ~bin)
     ("/usr/bin/systemctl" "--user" "start" "re-gent.service"))))

(defn build []
  (with-sh-dir "../re-gent"
    (sh "bash" "bin/binary.sh")))

(extend-type Hosts
  Regent
  (kill-agent
    ([this]
     (kill-agent this {}))
    ([this _]
     [this (run-hosts this (kill-script))]))
  (unregister-hosts [this]
    (let [registery-status (group-by zm/registered? (:hosts this))
          failures (mapv (fn [host] {:host host :code -1 :uuid (gen-uuid) :error {:out "host not registered"}}) (registery-status false))
          success (mapv (fn [host] {:host host :code 0}) (registery-status true))]
      (doseq [host (registery-status true)]
        (zm/unregister (zm/get-address host)))
      [this {:failure {-1 failures} :success success :hosts (:hosts this)}]))
  (start-agent
    ([this home]
     (start-agent this nil home))
    ([this _ home]
     [this (run-hosts this (start-script (used-port) home "info"))])))

(defn refer-regent []
  (require '[re-mote.repl.re-gent :as re-gent :refer (start-agent kill-agent build unregister-hosts)]))
