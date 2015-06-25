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

(ns celestial.api.stacks
  "Setting up clusteres of systems"
 )

(defmodel system 
  :env :string
  :type :string
  :user :string
  :machine {:type "Machine"} 
  :aws {:type "Aws" :description "An EC2 based system"}
  :physical {:type "Physical" :description "A physical machine"}
  :openstack {:type "Openstack" :description "An openstack based instance"}
  :proxmox {:type "Proxmox" :description "A Proxmox based system"}
  :vcenter {:type "Vcenter" :description "A vCenter based system"})
