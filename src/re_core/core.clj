(ns re-core.core)

(defprotocol Vm
  "A VM/Machine base API, implement this in order to add a new provider into re-core"
  (create [this] "Creates a VM, the VM should be up and and ready to accept ssh sessions post this step")
  (delete [this] "Deletes a VM")
  (start [this] "Starts an existing VM only if its not running, ssh should be up")
  (stop [this]  "Stops a VM only if it exists and running")
  (status [this] "Returns vm status (values defere between providers) false if it does not exists")
  (ip [this] "Instance IP address"))
