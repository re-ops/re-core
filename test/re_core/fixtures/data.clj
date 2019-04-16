(ns re-core.fixtures.data
  "Systems")

(def redis-kvm
  {:env :dev
   :owner "ronen"
   :machine {:hostname "red1" :user "re-ops" :domain "local"
             :os :ubuntu-18.04.2 :cpu 4 :ram 1}
   :kvm {:node :localhost}
   :type "redis"})

(def redis-lxc
  {:env :dev
   :owner "ronen"
   :machine {:hostname "red1" :user "root" :domain "local"
             :os :ubuntu-18.04.2 :cpu 4 :ram 1}
   :lxc {:node :localhost}
   :type "redis"})

(def redis-digital
  {:env :dev
   :owner "admin"
   :machine {:hostname "red1" :user "root"
             :domain "local" :os :ubuntu-18.04.2}
   :digital-ocean {:region "lon1" :size "512mb"
                   :private_networking false}
   :type "redis"})

(def redis-ec2
  {:env :dev
   :owner "admin"
   :machine {:hostname "red1" :user "ubuntu"
             :domain "local" :os :ubuntu-18.04.2}

   :aws {:instance-type "t2.micro"
         :key-name "enceladus"
         :endpoint "ec2.ap-southeast-2.amazonaws.com"
         :security-groups ["default"]
         :ebs-optimized false}
   :type "redis"})

(def redis-physical
  {:env :dev
   :owner "admin"
   :machine {:hostname "red1" :user "ubuntu"  :ip "1.2.3.4"
             :domain "local" :os :ubuntu-18.04.2}
   :physical {:mac "9a:07:e4:bc:79:df"
              :broadcast "192.168.0.255"}
   :type "redis"})

(def redis-type
  {:puppet {:src "/home/ronen/code/boxes/redis-sandbox/"
            :tar "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.5.2.tar.gz"
            :args []}

   :description "Redis Database"
   :type "redis"})

(def volume {:device "vdb" :type "qcow2" :size 100 :clear true :pool :default :name "foo.img"})
