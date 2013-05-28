(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns celestial.core)

(defprotocol Vm
  "A VM/Machine base API, implement this in order to add a new provider into celestial"
  (create [this] "Creates a VM, the VM should be upand and ready to accept ssh sessions post this step")  
  (delete [this] "Deletes a VM")  
  (start [this] "Starts an existing VM, ssh should be up")  
  (stop [this]  "Stops a VM")
  (status [this] 
    "Returns vm status (values defere between providers) false if it does not exists"))

(defprotocol Provision
  "A provisioner (puppet/chef) base API, implement this to add more provisioners"
  (apply- [this] "applies provisioner"))


(defprotocol Remoter
  "Remote automation (capistrano, fabric and supernal) base api"
  (setup [this] "Sets up this remoter (pulling code, etc..)")
  (run [this ] "executes a task on remote hosts with")
  (cleanup [this] "Cleans up (deletes local source etc..)"))
