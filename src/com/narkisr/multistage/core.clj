(ns com.narkisr.multistage.core)

(defprotocol Vm
  (create [this])  
  (delete [this])  
  (start [this])  
  (stop [this])  
  )


