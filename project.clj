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
                 [bigml/closchema "0.3.0-SNAPSHOT"]
                 [org.clojure/core.incubator "0.1.2"]
                 [clj-http "0.6.4"]]
  
  :plugins  [[lein-tarsier "0.10.0"]]

  :aot [com.narkisr.proxmox.provider]

  :test-selectors {:default (complement :integration)
                   :integration :integration
                   :all (constantly true)}

)
