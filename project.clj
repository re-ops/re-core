(defproject celestial "0.1.0-SNAPSHOT"
  :description "A launching pad for virtualized applications"
  :url ""
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [clj-ssh "0.5.0"]
                 [clj-config "0.2.0"]
                 [cheshire "5.0.1"]
                 [com.taoensso/timbre "1.4.0"]
                 [narkisr/closchema "0.3.0-SNAPSHOT"]
                 [org.clojure/core.incubator "0.1.2"]
                 [slingshot "0.10.3"]
                 [clj-http "0.6.4"]
                 [compojure "1.1.1"]
                 [ring "1.0.2"]
                 [fogus/ring-edn "0.1.0"] ]
  
  :plugins  [[lein-tarsier "0.10.0"]  [lein-ring "0.7.3"]]

  :ring {:handler com.narkisr.celestial.api/app :auto-reload? true}

  :aot [com.narkisr.proxmox.provider com.narkisr.celestial.puppet-standalone]

  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}

  :aliases  {"reload"  ["run" "-m" "com.narkisr.celestial.tasks" "reload" "systems/baseline.edn" "proxmox"]
             "puppetize"  ["run" "-m" "com.narkisr.celestial.tasks" "puppetize" "systems/baseline.edn"]}
  
)
