(ns celestial.core)

(defprotocol Vm
  (create [this])  
  (delete [this])  
  (start [this])  
  (stop [this])
  (status [this] 
    "Returns vm status (values defere between providers) false if it does not exists"))

(defprotocol Provision
  (apply- [this]))

(defprotocol Registry 
  (register [this machine])
  (un-register [this machine]))

