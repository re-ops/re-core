(defproject celestial "0.13.3"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/celestial-ops/celestial-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-config "0.2.0" ]
                 ; utils
                 [me.raynes/fs "1.4.6"]
                 [robert/hooke "1.3.0"]
                 [org.clojure/core.incubator "0.1.3"]
                 [slingshot "0.10.3" ]
                 [org.clojure/core.memoize "0.5.7"]
                 [org.flatland/useful "0.10.3"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/java.data "0.1.1"]
                 ; templating
                 [selmer "0.8.2"]
                 [com.palletops/stevedore "0.8.0-beta.5"]
                 [camel-snake-kebab "0.1.2"]
                 ; logging
                 [com.narkisr/gelfino-client "0.8.1"]
                 [com.taoensso/timbre "4.1.4"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.codehaus.groovy/groovy "2.4.3"]
                 ; hooks/remoting
                 [clj-http "0.7.6"]
                 [http-kit "2.1.18"]
                 [supernal "0.6.1"]
                 [conjul "0.0.2"]
                 ;api
                 [swag "0.2.7"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/data.json "0.2.2" ]
                 ; ring
                 [ring-middleware-format "0.5.0"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring "1.3.2"]
                 [compojure "1.3.4" :exclusions  [ring/ring-core]]
                 ; ring security
                 [com.cemerick/friend "0.2.1"]
                 [ring/ring-session-timeout "0.1.0"]
                 [ring/ring-headers "0.1.0"]
                 ; hypervisors
                 [narkisr/digitalocean "1.3"]
                 [org.pacesys/openstack4j "2.0.3"]
                 [amazonica "0.3.13" ]
                 ; gce
                 [com.google.http-client/google-http-client-jackson2 "1.21.0"]
                 [com.google.apis/google-api-services-compute "v1-rev88-1.21.0"]
                 [com.google.api-client/google-api-client "1.21.0"]
                 [com.google.oauth-client/google-oauth-client-java6 "1.21.0"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.21.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.6.4"]
                 ; libvirt
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.1"]
                 [net.java.dev.jna/jna "4.2.0"]
                 [org.libvirt/libvirt "0.5.1"]
                 ; persistency and model
                 [com.google.guava/guava "18.0"]
                 [clojurewerkz/elastisch "3.0.0-beta1"]
                 [puny "0.3.0"]
                 [com.taoensso/carmine "2.11.1"]
                 [commons-codec "1.10"]
                 [substantiation "0.3.1"]
                 [fogus/minderbinder "0.2.0"]
                 [org.clojure/core.logic "0.8.10"]
                 ; scheduling
                 [jarohen/chime "0.1.6" :exclusions [org.clojure/core.async]]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 ; remote managment
                 [org.clojure/tools.nrepl "0.2.10"]
                 ; metrics
                 [metrics-clojure "2.5.1"]
                 [metrics-clojure-health "2.5.1"]
                 [metrics-clojure-jvm "2.5.1"]
                 [metrics-clojure-ring "2.5.1"]
               ]

  :exclusions [org.clojure/clojure com.taoensso/timbre commons-codec]

  :plugins  [[jonase/eastwood "0.2.1"]
             [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0" ] [self-build "0.0.9"]
             [lein-tag "0.1.0"] [lein-set-version "0.3.0"]
             [topping "0.0.2"]]

  :bin {:name "celestial"}

  :profiles {
     :populate {
        :source-paths ["data"]
        :test-paths ["test"]
        :dependencies [[org.clojure/test.check "0.7.0"]]

     }

     :refresh {
        :repl-options {
          :init-ns user
          :timeout 120000
        }

        :dependencies [[org.clojure/tools.namespace "0.2.10"] [midje "1.6.3"]
                       [redl "0.2.4"] [org.clojure/tools.trace "0.7.8"]]
        :injections  [(require '[redl core complete])]
        :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"]
        :source-paths  ["dev"]
        :test-paths  []
 
     }

     :dev {
        :repl-options {
          :timeout 120000
        }

        :aot [remote.capistrano remote.ruby freenas.provider
              aws.provider physical.provider openstack.provider
              celestial.core celestial.puppet-standalone celestial.launch]
 
        :test-paths ["test" "data"]
        :source-paths  ["dev"]
        :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"]
        :dependencies [[ring-mock "0.1.5"] [midje "1.6.3"]
                       [org.clojure/tools.trace "0.7.8"]
                       [org.clojure/test.check "0.7.0"]]
        :plugins [[lein-midje "3.1.3"]]
        :jvm-opts ~(vec (map (fn [[p v]] (str "-D" (name p) "=" v)) {:disable-conf "true" }))
        :set-version {
           :updates [ 
             {:path "project.clj" :search-regex #"\"target\/celestial-\d+\.\d+\.\d+\.jar"}
             {:path "src/celestial/common.clj" :search-regex #"\"\d+\.\d+\.\d+\""}]}

        :main celestial.launch
      }

     :prod {
        :resource-paths  ["src/main/resources/" "pkg/etc/celestial/"] 


        :aot [remote.capistrano remote.ruby freenas.provider
              aws.provider physical.provider openstack.provider
              celestial.core celestial.puppet-standalone celestial.launch]

        :main celestial.launch
      }
    }


  :aliases {"celestial" ["with-profile" "prod" "do" "compile," "trampoline" "run"]
            "remote-repl" ["repl" ":connect" "celestial:7888"]
            "autotest" ["midje" ":autotest" ":filter" "-integration"] 
            "runtest" ["midje" ":filter" "-integration"] 
            "populate" ["with-profile" "populate" "do" "run" "-m" "celestial.fixtures.populate"]
            ; https://github.com/stuartsierra/reloaded workflow
            "dev-repl" ["with-profile" "refresh" "do" "clean," "repl"] 
            }


  :repositories  {"bintray"  "http://dl.bintray.com/content/garkisr/narkisr-jars"
                  "sonatype" "http://oss.sonatype.org/content/repositories/releases"
                  "libvirt-org" "http://libvirt.org/maven2"}

  :topping {
      :service "celestial"
      :app {:app-name "celestial" :src "target/celestial-0.13.3.jar"}
      :env {:roles {:remote #{{:host "celestial" :user "ubuntu" :sudo true}}}}
   }

  :resource-paths  ["src/main/resources/"]
  :source-paths  ["src"]
  :target-path "target/"
  :test-paths  []
  :repl-options { }
)
