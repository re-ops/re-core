(ns re-core.fixtures.data
  (:require
   [re-cipes.profiles]))

(def redis-kvm
  {:machine {:hostname "red1" :user "re-ops" :domain "local"
             :os :ubuntu-20.04 :cpu 4 :ram 1}
   :kvm {:node :localhost}
   :type :redis
   :description "redis kvm instance"})

(def redis-lxc
  {:machine {:hostname "red1" :user "root" :domain "local"
             :os :ubuntu-20.04 :cpu 4 :ram 1}
   :lxc {:node :localhost}
   :type :redis
   :description "redis lxc system"})

(def redis-digital
  {:machine {:hostname "red1" :user "root"
             :domain "local" :os :ubuntu-20.04}
   :digital-ocean {:region "lon1" :size "512mb"
                   :private_networking false}
   :type :redis
   :description "redis digital system"})

(def redis-physical
  {:machine {:hostname "red1" :user "ubuntu"  :ip "1.2.3.4"
             :domain "local" :os :ubuntu-20.04}
   :physical {:mac "9a:07:e4:bc:79:df"
              :broadcast "192.168.0.255"}
   :type :redis
   :description "A Physical redis system"})

(def redis-type
  {:cog {:src "/home/ronen/code/re-ops/re-cipes/resources"
         :args []}

   :description "Redis Database"
   :type :redis})

(def volume {:device "vdb" :type "qcow2" :size 100 :clear true :pool :default :name "foo.img"})
