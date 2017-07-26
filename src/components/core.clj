(ns components.core
  "A less intrusive component model")

(defprotocol Lifecyle
  (setup [this] "A one time setup (per application run) code")
  (start [this] "Start this component")
  (stop  [this] "Stop this component"))

(defn start-all
  "start all components"
  [cs]
  (doseq [[k c] cs] (.start c)))

(defn stop-all
  "stops all components"
  [cs]
  (doseq [[k c] cs] (.stop c)))

(defn setup-all
  "setup all components"
  [cs]
  (doseq [[k c] cs] (.setup c)))
