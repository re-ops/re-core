(defproject celestial "0.0.1"
  :description "A launching pad for virtualized applications"
  :url ""
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [clj-ssh "0.5.0"]
                 [clj-config "0.2.0"]
                 [prismatic/plumbing "0.0.1"]
                 [cheshire "5.0.1"]
                 [com.taoensso/timbre "1.4.0"]
                 [narkisr/closchema "0.3.0-SNAPSHOT"]
                 [org.clojure/core.incubator "0.1.2"]
                 [slingshot "0.10.3"]
                 [clj-http "0.6.4"]
                 [clj-yaml "0.4.0"]
                 [compojure "1.1.1"]
                 [ring "1.0.2"]
                 [org.clojure/data.json "0.2.1"]
                 [com.taoensso/carmine "1.5.0"]
                 [org.clojure/core.memoize "0.5.2"]
                 [metrics-clojure "0.9.2"]
                 [trammel "0.7.0"]
                 [mississippi "1.0.1"]
                 [metrics-clojure-ring "0.9.2"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [fogus/ring-edn "0.1.0"] ]

  :plugins  [[lein-autoexpect "0.2.5"]  [jonase/eastwood "0.0.2"] [lein-tarsier "0.10.0"]
             [lein-ring "0.7.3"] [lein-expectations "0.0.7"]]

  :profiles {:dev {:dependencies [[ring-mock "0.1.3"] [expectations "1.4.24"] [junit/junit "4.8.1"] ]}}
                 

  :aot [proxmox.provider 
        celestial.puppet-standalone 
        celestial.api]

  :test-selectors {:default #(not-any? % [:proxmox :redis :integration :puppet]) 
                   :redis :redis
                   :proxmox :proxmox
                   :integration :integration
                   :all (constantly true)}

  :aliases  
  {"reload"  ["run" "-m" "celestial.tasks" "reload" "systems/baseline.edn" "proxmox"]
   "puppetize"  ["run" "-m" "celestial.tasks" "puppetize" "systems/baseline.edn"]}

  :main celestial.api
  )
