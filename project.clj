(defproject celestial "0.1.9"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/celestial-ops/celestial-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [clj-config "0.2.0" ]
                 [com.vmware/vijava "5.1" :exclusions [xml-apis]]
                 [me.raynes/fs "1.4.5"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [de.ubercode.clostache/clostache "1.3.1"]
                 [bouncer "0.2.3-beta2"]
                 [com.taoensso/timbre "1.6.0"]
                 [robert/hooke "1.3.0"]
                 [com.narkisr/gelfino-client "0.4.2"]
                 [org.clojure/core.incubator "0.1.3"]
                 [slingshot "0.10.3" ]
                 [clj-http "0.6.5"]
                 [swag "0.2.2"]
                 [puny "0.1.2"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/data.json "0.2.2" ]
                 [com.taoensso/carmine "2.0.0"]
                 [org.clojure/core.memoize "0.5.2" :exclusions [org.clojure/core.cache]]
                 [narkisr/clj-aws-ec2 "0.2.1" :exclusions  [org.codehaus.jackson/jackson-core-asl]]
                 [narkisr/trammel "0.8.0-freez"]
                 [org.flatland/useful "0.10.3"]
                 [substantiation "0.0.3"]
                 [fogus/minderbinder "0.2.0"]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.codehaus.groovy/groovy "2.1.6"]
                 [supernal "0.2.8"]
                 [ring-middleware-format "0.3.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring "1.2.0"]
                 [metrics-clojure "1.0.1"]
                 [metrics-clojure-ring "1.0.1"]
                 [compojure "1.1.5" :exclusions  [ring/ring-core]]
                 [com.cemerick/friend "0.1.5"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/java.data "0.1.1"]
                 [org.nmap4j/org.nmap4j "1.0.4"]
                 ; luminusweb
                 [lib-noir "0.6.6"]
                 [clabango "0.5"]
                 [markdown-clj "0.9.28"]
                 ]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.0.2"] [lein-midje "3.0.0"] [lein-ancient "0.4.2"]
             [lein-tar "2.0.0" ] [lein-tag "0.1.0"] [lein-set-version "0.3.0"] [topping "0.0.2"]]

  :bin {:name "celestial"}

  :profiles {:dev {
               :source-paths  ["dev"]
               :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"]
               :dependencies [[org.clojure/tools.trace "0.7.5"] [ring-mock "0.1.5"]
                              [midje "1.5.1" :exclusions [org.clojure/core.unify]]
                              [junit/junit "4.11"]]
               :jvm-opts ~(vec (map (fn [[p v]] (str "-D" (name p) "=" v)) {:disable-conf "true" }))
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

  :aliases {"celestial" 
            [ "with-profile" "prod" "do" "compile," "trampoline" "run"]
            "autotest"
            ["midje" ":autotest" ":filter" "-integration"] 
            "runtest"
            ["midje" ":filter" "-integration"] 
            }

  :aot [supernal.launch capistrano.remoter proxmox.provider vc.provider
        aws.provider celestial.core celestial.puppet-standalone celestial.launch]

  :repositories  {"bintray"  "http://dl.bintray.com/content/narkisr/narkisr-jars"
                  "sonatype" "http://oss.sonatype.org/content/repositories/releases"}

  :topping {
      :service "celestial"
      :app {:app-name "celestial" :src "target/celestial-0.1.9.jar"}
      :env {:roles {:celestial #{{:host "celestial" :user "ronen" :sudo true}}}}
  } 

  :resource-paths  ["src/main/resources/"]
  :source-paths  ["web" "src"]
  :target-path "target/"

  :main celestial.launch
  )
