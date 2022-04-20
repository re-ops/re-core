(ns re-mote.repl.desktop
  "Desktop oriented operations"
  (:require
   [re-mote.ssh.pipeline :refer (run-hosts)]
   [clojure.core.strint :refer (<<)]
   [pallet.stevedore :refer (script)]
   [taoensso.timbre :refer (refer-timbre)]
   re-mote.repl.base)
  (:import [re_mote.repl.base Hosts]))

(refer-timbre)

(defprotocol Desktop
  (browse
    [this url]
    [this m url])
  (writer
    [this doc]
    [this m doc]))

(defn writer-script
  "Launch a docment in libreoffice-writer in view only mode"
  [doc]
  (let [cmd (<< "\"/usr/bin/libreoffice --view '~{doc}' &\"")]
    (script
     ("export" (set! DISPLAY ":0"))
     ("nohup" "sh" "-c" ~cmd))))

(defn chrome-script
  "Launch chrome in full screen"
  [url]
  (script
   ("export" (set! DISPLAY ":0"))
   ("/usr/bin/pgrep" "chrome" ">/dev/null" "2>&1")
   (if (= "$?" 0)
     ("nohup" "sh" "-c" "\"/usr/bin/google-chrome" "'" ~url "'" "&\"")
     ("nohup" "sh" "-c" "\"/usr/bin/google-chrome" "--start-fullscreen" "'" ~url "'" "&\""))))

(extend-type Hosts
  Desktop
  (browse
    ([this url]
     (browse this nil url))
    ([this _ url]
     [this (run-hosts this (chrome-script url))]))
  (writer
    ([this doc]
     (writer this nil doc))
    ([this _ doc]
     [this (run-hosts this (writer-script doc))])))

(defn refer-desktop []
  (require '[re-mote.repl.desktop :as dsk :refer (browse writer)]))
