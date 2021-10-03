(ns re-mote.repl.zero.desktop
  "Desktop oriented operations"
  (:require
   [re-cog.scripts.desktop :refer (fullscreen-chrome librewriter xmonad killall xdot-key xdot-type)]
   [re-mote.zero.pipeline :refer (run-hosts)]
   [clojure.core.strint :refer (<<)]
   [re-mote.repl.base :refer (refer-base)]
   [re-mote.repl.publish :refer (email)]
   [re-cog.scripts.common :refer (shell-args shell)]
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
    [this m doc])
  (tile [this])
  (kill- [this proc])
  (type- [this s])
  (send-key [this ks]))

(extend-type Hosts
  Desktop
  (browse
    ([this url]
     (browse this nil url))
    ([this _ url]
     [this (run-hosts this shell (shell-args (fullscreen-chrome url) :wait? false) [10 :second])]))
  (writer
    ([this doc]
     (writer this nil doc))
    ([this _ doc]
     [this (run-hosts this shell (shell-args (librewriter doc) :wait? false))]))
  (tile
    ([this]
     [this (run-hosts this shell (shell-args (xmonad) :wait? false))]))
  (kill-
    ([this proc]
     [this (run-hosts this shell (shell-args (killall proc)))]))
  (type-
    ([this s]
     [this (run-hosts this shell (shell-args (xdot-type s)))]))
  (send-key
    ([this ks]
     [this (run-hosts this shell (shell-args (xdot-key ks)))])))

(defn refer-desktop []
  (require '[re-mote.repl.zero.desktop :as dsk :refer (browse writer type- send-key tile kill-)]))

(comment
  (shell-args (fullscreen-chrome "http://google.com") :wait? false :cached? false))
