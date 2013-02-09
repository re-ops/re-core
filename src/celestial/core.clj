(ns celestial.core)

(defprotocol Vm
  (create [this])  
  (delete [this])  
  (start [this])  
  (stop [this])
  (status [this]))

(defprotocol Provision
  (apply- [this]))

(defprotocol Registry 
  (register [this machine])
  (un-register [this machine]))
