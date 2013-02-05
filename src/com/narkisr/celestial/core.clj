(ns com.narkisr.celestial.core)

(defprotocol Vm
  (create [this])  
  (delete [this])  
  (start [this])  
  (stop [this])  
  )


