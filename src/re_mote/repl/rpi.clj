(ns re-mote.repl.rpi
  (:require
   [re-mote.repl.output :refer (refer-out)]
   [re-mote.repl.base :refer (refer-base)]
   [re-cog.scripts.rpi :refer (set-screen)]))

(refer-base)
(refer-out)

(defn screen-on [hs]
  (run (exec hs (set-screen :on)) | (pretty "screen-on")))

(defn screen-off [hs]
  (run (exec hs (set-screen :off)) | (pretty "screen-on")))
