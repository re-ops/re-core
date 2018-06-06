(ns re-core.config
  "Celetial configuration info"
  (:require
   [clj-config.core :as conf]
   [subs.core :refer (validate! combine validation when-not-nil every-kv)]
   [clojure.pprint :refer (pprint)]
   [taoensso.timbre :refer (merge-config! info)]
   [taoensso.timbre.appenders.core :refer (spit-appender)]
   [clojure.core.strint :refer (<<)]
   [clojure.java.io :refer (file)]))

(validation :elasticsearch*
            (every-kv {:host #{:required :String} :port #{:required :Integer}}))

(def base-v {:ssh {:private-key-path #{:required :String}}
             :elasticsearch #{:required :elasticsearch*}})

(def levels #{:trace :debug :info :error})

(validation :levels (when-not-nil levels (<< "level must be either ~{levels}")))

(def reset-options #{:stop :start})

(validation :reset-options
            (when-not-nil reset-options (<< "type must be either ~{reset-options}")))

(def ^{:doc "Base config validation"} re-core-v
  {:re-core
   {:port #{:required :number} :https-port #{:required :number}
    :log {:level #{:required :levels}
          :path #{:required :String}}}})

(validation :node*
            (every-kv {:username #{:required :String} :password #{:required :String}
                       :host #{:required :String} :ssh-port #{:required :number}}))

(def flavors #{:redhat :debian :windows})

(validation :flavor
            (when-not-nil flavors  (<< "flavor must be either ~{flavors}")))

(validation :template*
            (every-kv {:template #{:required :String} :flavor #{:required :Keyword :flavor}}))

(validation :kvm-node*
            (every-kv {:username #{:required :String} :host #{:required :String}
                       :port #{:required :number} :pools #{:Map}}))

(def ^{:doc "kvm validation"} kvm-v
  {:kvm {:nodes #{:required :kvm-node*} :ostemplates #{:template*}}})

(def ^{:doc "digital ocean section validation"} digital-v
  {:digital-ocean {:token #{:required :String}
                   :ssh-key #{:required :String}
                   :ostemplates #{:required :Map}}})

(def ^{:doc "aws section validation"} aws-v {:aws {:access-key #{:required :String} :secret-key #{:required :String}
                                                   :default-vpc {:vpc-id #{:required :String} :subnet-id #{:required :String} :assign-public #{:required :Boolean}}}})

(validation :managment
            (when-not-nil #{:floating :network} "must be either floating or network"))

(defn hypervisor-validations
  "find relevant hypervisor validations per env"
  [hypervisor]
  (let [hvs [kvm-v aws-v digital-v] ks (map (comp first keys) hvs)
        envs (map (fn [v] (fn [env] {:hypervisor {env v}})) hvs)]
    (first
     (map (fn [[e hs]] (map #(((zipmap ks envs) %) e) (filter (into #{} ks) (keys hs)))) hypervisor))))

(defn re-core-validations [{:keys [log job] :as re-core}]
  (if (contains? log :gelf)
    (combine re-core-v)
    re-core-v))

(defn validate-conf
  "applies all validations on a configration map"
  [{:keys [hypervisor re-core] :as c}]
  (validate! c
             (apply combine
                    (into [base-v (re-core-validations re-core)] (hypervisor-validations hypervisor)))))
