(ns re-core.core)

(defprotocol Vm
  "A VM/Machine base API, implement this in order to add a new provider into re-core"
  (create [this] "Creates a VM, the VM should be up and and ready to accept ssh sessions post this step")  
  (delete [this] "Deletes a VM")  
  (start [this] "Starts an existing VM only if its not running, ssh should be up")  
  (stop [this]  "Stops a VM only if it exists and running")
  (status [this] "Returns vm status (values defere between providers) false if it does not exists")
  (ip [this] "Instance IP address") 
  )



(defprotocol Provision
  "A provisioner (puppet/chef) base API, implement this to add more provisioners"
  (apply- [this] "applies provisioner"))


(defprotocol Remoter
  "Remote automation (capistrano, fabric and re-mote) base api"
  (setup [this] "Sets up this remoter (pulling code, etc..)")
  (run [this ] "executes a task on remote hosts with")
  (cleanup [this] "Cleans up (deletes local source etc..)"))
