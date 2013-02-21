# Model

Celetial model is as generic as possible and is composed of:

* A machine is the virtualized/physical medium we work on.
* A type (for example a mysql server type) that effects what will be installed on the machine, a type can be binded to a module.
* module is the provision info require to setup this machine (a type points to module)
* host is the primary key in accessing all of these and is unique, a host has a type and a machine associated to it

Open questions:

* How to seperated specific properties from general ones, for example:


```clojure
{:machine
 {:vmid 203 :cpus  4 :memory  4096 :disk 30
  :hostname  "redis-local" :ip_address "192.168.5.203"  :host "192.168.5.203"
  :ostemplate "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
  :password "foobar1" 
  :hypervisor "proxmox"
  :features ["nfs:on"]}
} ```

Here we keep both a proxmox specific properties like ip_address and general ones like :host

One options is:

{:machine

 :base { ; general options
   :host "192.168.5.203" :cpus  4 :memory  4096 :disk 30
 }

 :ext {; specific
  :vmid 203 
  :hostname  "redis-local" :ip_address "192.168.5.203"  
  :ostemplate "local:vztmpl/ubuntu-12.04-puppet_3-x86_64.tar.gz"
  :password "foobar1" 
  :hypervisor "proxmox"
  :features ["nfs:on"]}
}
