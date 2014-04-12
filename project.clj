(defproject celestial "0.5.1"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/celestial-ops/celestial-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [clj-config "0.2.0" ]
                 [com.vmware/vijava "5.1" :exclusions [xml-apis]]
                 [me.raynes/fs "1.4.5"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [com.taoensso/timbre "2.6.3"]
                 [robert/hooke "1.3.0"]
                 [com.narkisr/gelfino-client "0.6.0"]
                 [org.clojure/core.incubator "0.1.3"]
                 [slingshot "0.10.3" ]
                 [clj-http "0.7.6"]
                 [swag "0.2.7"]
                 [puny "0.2.5"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/data.json "0.2.2" ]
                 [com.taoensso/carmine "2.0.0"]
                 [org.clojure/core.memoize "0.5.2" :exclusions [org.clojure/core.cache]]
                 [amazonica "0.1.29"]
                 [narkisr/trammel "0.8.0-freez"]
                 [org.flatland/useful "0.10.3"]
                 [substantiation "0.1.3"]
                 [fogus/minderbinder "0.2.0"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.codehaus.groovy/groovy "2.1.6"]
                 [supernal "0.4.0"]
                 [ring-middleware-format "0.3.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring "1.2.0"]
                 [metrics-clojure "1.0.1"]
                 [metrics-clojure-ring "1.0.1"]
                 [compojure "1.1.5" :exclusions  [ring/ring-core]]
                 [com.cemerick/friend "0.2.0"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/java.data "0.1.1"]
                 [org.nmap4j/org.nmap4j "1.0.4"]
                 [selmer "0.3.4"]; for templating
                 [com.palletops/stevedore "0.8.0-beta.5"]
                 [camel-snake-kebab "0.1.2"]
                 ; elastic search 
                 [clojurewerkz/elastisch "2.0.0-beta3"]
                 ]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.1.0"] 
             [self-build "0.0.5"]
             [lein-ancient "0.4.2"] [lein-tar "2.0.0" ]
             [lein-tag "0.1.0"] [lein-set-version "0.3.0"]
             [topping "0.0.2"] [self-build "0.0.3"]]

  :bin {:name "celestial"}

  :profiles {
             :refresh {
                :dependencies [[org.clojure/tools.namespace "0.2.4"] 
                               [org.clojure/tools.trace "0.7.5"]
                               [midje "1.5.1" :exclusions [org.clojure/core.unify]]
                               [clojure-complete "0.2.3"] [redl "0.2.0"]]
                :injections  [(require '[redl core complete])]
                :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"]
                :source-paths  ["dev" "src"]
                :test-paths  []
                :jvm-opts ["-XX:MaxPermSize=256m"]
             }

             :dev {
               :aot [supernal.launch remote.capistrano proxmox.provider vc.provider
                     aws.provider celestial.core celestial.puppet-standalone celestial.launch]
 
               :test-paths ["test" "data"]
               :source-paths  ["dev"]
               :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"]
               :dependencies [[org.clojure/tools.trace "0.7.5"] [ring-mock "0.1.5"]
                              [midje "1.5.1" :exclusions [org.clojure/core.unify]]
                              [junit/junit "4.11"] [reiddraper/simple-check "0.5.0"]]
               :plugins [[lein-midje "3.0.0"]]
               :jvm-opts ~(into (vec (map (fn [[p v]] (str "-D" (name p) "=" v)) {:disable-conf "true" })) ["-XX:MaxPermSize=256m"])
               :set-version {
                  :updates [ 
                    {:path "project.clj" :search-regex #"\"target\/celestial-\d+\.\d+\.\d+\.jar"}
                    {:path "src/celestial/common.clj" :search-regex #"\"\d+\.\d+\.\d+\""}]}}

              :prod {
                :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"] 
              } 
             }

  :repl-options {
    :init-ns user               
  }

  :aliases {"celestial" [ "with-profile" "prod" "do" "compile," "trampoline" "run"]
            "remote-repl" ["repl" ":connect" "celestial:7888"]
            "autotest" ["midje" ":autotest" ":filter" "-integration"] 
            "runtest" ["midje" ":filter" "-integration"] 
            "populate" ["run" "-m" "celestial.fixtures.populate"]
            ; https://github.com/stuartsierra/reloaded workflow
            "dev-repl" ["with-profile" "refresh" "do" "repl"] 
            }

  
  :repositories  {"bintray"  "http://dl.bintray.com/content/narkisr/narkisr-jars"
                  "sonatype" "http://oss.sonatype.org/content/repositories/releases"}

  :test-paths []
  :topping {
      :service "celestial"
      :app {:app-name "celestial" :src "target/celestial-0.5.1.jar"}
      :env {:roles {:celestial #{{:host "tk-celestial" :user "ronen" :sudo true}}}}
  } 

  :resource-paths  ["src/main/resources/"]
  :source-paths  ["src"]
  :target-path "target/"

  :main celestial.launch
  )
