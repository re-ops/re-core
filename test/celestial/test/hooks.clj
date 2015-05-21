(ns celestial.test.hooks
  "Testing misc hooks like dns and hubot"
  (:require 
    [clojure.java.shell :refer (sh)]
    [me.raynes.fs :refer (temp-file)]
    [hooks.dnsmasq :refer (update-dns hosts sudo)]
    [celestial.common :refer (import-logging)]
    [celestial.persistency.systems :as s] 
    [supernal.sshj :refer (execute)] 
    )
  (:use midje.sweet))

(import-logging)

(defn stubed-execute [s r] 
  (let [run (temp-file "run")]
    (when-not (.contains s "dnsmasq")
      (spit run s) 
      (assert (:exit (sh "bash" (str run))) 0))))

(defn fail 
   "failing on purpouse" 
   [s & r]
  (throw (RuntimeException. "somthing bad")) 
  )

(let [hosts-file (temp-file "hosts") machine {:domain "local" :ip "1.2.3.4" :hostname "iobar" }]
  (with-redefs [hosts (agent hosts-file) execute stubed-execute 
                s/get-system (fn [_] {:machine machine}) sudo ""]
    (fact "adding a host on create" :integration :agent
          (update-dns {:event :success :workflow :create :system-id 1
                       :domain "local" :dnsmasq "192.168.1.1" :user "foo" }) => truthy
          (await-for 1000 hosts) => true
          (agent-error hosts) => nil
          (slurp hosts-file) => "1.2.3.4 iobar iobar.local\n")

    (fact "removing a host on stop" :integration :agent
          
          (update-dns {:event :success :workflow :stop :system-id 1 
                       :domain "local" :dnsmasq "192.168.1.1" :user "foo" :machine machine}) => truthy
          (await-for 1000 hosts) => true
          (agent-error hosts) => nil
          (slurp hosts-file) => "") 

    (fact "failing execution not in action" :integration :agent
          
          (send-off hosts fail)
          (update-dns {:event :success :workflow :create :system-id 1
                       :domain "local" :dnsmasq "192.168.1.1" :user "foo" }) => truthy
          (await-for 8000 hosts) => true
          (agent-error hosts) => nil)   

    (with-redefs [execute fail]
      (fact "failing execution in action" :integration :agent
          
          (update-dns {:event :success :workflow :create :system-id 1
                       :domain "local" :dnsmasq "192.168.1.1" :user "foo" }) => truthy
          (await-for 8000 hosts) => true
          (agent-error hosts) => nil))))
