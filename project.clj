(defproject re-core "0.9.0"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/re-core-ops/re-core-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-config "0.2.0" ]

                 ; utils
                 [me.raynes/fs "1.4.6"]
                 [robert/hooke "1.3.0"]
                 [org.clojure/core.incubator "0.1.4"]
                 [org.clojure/core.memoize "0.5.9"]
                 [org.flatland/useful "0.11.5"]
                 [org.clojure/tools.macro "0.1.5"]
                 [org.clojure/java.data "0.1.1"]

                 ; templating
                 [selmer "0.8.2"]
                 [com.palletops/stevedore "0.8.0-beta.7"]
                 [camel-snake-kebab "0.1.2"]

                 ; logging / profiling
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.7"]
                 [com.taoensso/tufte "1.1.1"]

                 ; re-ops
                 [re-mote "0.9.6"]
                 [re-share "0.9.12"]

                 ; Elasticsearc
                 [rubber "0.3.2"]

                 ;api
                 [clj-yaml "0.4.0"]
                 [org.clojure/data.json "0.2.6" ]

                 ; hypervisors
                 [narkisr/digitalocean "1.3"]
                 [amazonica "0.3.94" :exclusions [com.taoensso/nippy]]

                 ; libvirt
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [net.java.dev.jna/jna "4.2.0"]
                 [org.libvirt/libvirt "0.5.1"]

                 ; model
                 [com.rpl/specter "1.0.3"]
                 [com.brunobonacci/safely "0.2.4"]
                 [com.google.guava/guava "18.0"]
                 [commons-codec "1.10"]
                 [substantiation "0.4.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.6.4"]

                 ; es
                 [cc.qbits/spandex "0.6.0" :exclusions [org.clojure/clojure]]
                 [org.apache.httpcomponents/httpclient "4.5.2"]

                 ; queue
                 [factual/durable-queue "0.1.5"]
                 [cc.qbits/knit "0.3.1"]

                 ; timeunits
                 [fogus/minderbinder "0.2.0"]

                 ; scheduling
                 [jarohen/chime "0.2.0" :exclusions [org.clojure/core.async]]
                 [org.clojure/core.async "0.3.443"]

                 ; repl
                 [org.clojure/tools.nrepl "0.2.10"]
                 [io.aviso/pretty "0.1.34"]

                 ; metrics
                 [metrics-clojure "2.10.0"]
                 [metrics-clojure-health "2.10.0"]
                 [metrics-clojure-jvm "2.10.0"]

                 [potemkin "0.4.2"] ; see http://bit.ly/2mVr1sI

                 ; wiring
                 [mount "0.1.13"]
               ]

  :exclusions [org.clojure/clojure com.taoensso/timbre commons-codec]

  :plugins  [[jonase/eastwood "0.2.4"]
             [mvxcvi/whidbey "1.3.1"]
             [lein-cljfmt "0.5.6"]
             [lein-kibit "0.1.5"]
             [lein-ancient "0.6.15" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0" ]
             [self-build "0.0.9"]
             [lein-tag "0.1.0"]
             [venantius/ultra "0.5.2"]
             [lein-set-version "0.3.0"]]

  :bin {:name "re-core"}

  :profiles {
     :codox {
        :dependencies [[org.clojure/tools.reader "1.1.0"]
                       [codox-theme-rdash "0.1.2"]]
              :plugins [[lein-codox "0.10.3"]]
              :codox {:project {:name "re-core"}
                      :themes [:rdash]
                      :source-paths ["src"]
                      :source-uri "https://github.com/re-ops/re-core/blob/master/{filepath}#L{line}"
              }
     }

     :dev {
        :source-paths  ["dev" "test" "data"]
        :resource-paths  ["src/main/resources/"]
        :dependencies [
          [cljfmt "0.5.2"]
          [org.clojure/tools.trace "0.7.9"]
          [org.clojure/test.check "0.7.0"]
        ]
        :set-version {
           :updates [
             {:path "project.clj" :search-regex #"\"target\/re-core-\d+\.\d+\.\d+\.jar"}
             {:path "src/re-core/common.clj" :search-regex #"\"\d+\.\d+\.\d+\""}]}

     }
    }


  :jvm-opts ^:replace ["-Djava.library.path=/usr/lib:/usr/local/lib" "-Xms4g" "-Xmx4g"]

  :aliases {
      "test" [
        "test" ":only"
        "re-core.test.aws" "re-core.test.kvm" "re-core.test.digital"
        "re-core.test.physical" "re-core.test.validation" "re-core.test.provider"
      ]

      "integration" [
       "test" ":only"
       "re-core.integration.es.jobs"
       "re-core.integration.es.systems"
      ]
      "travis" [
        "with-profile" "test" "do" "test," "integration," "cljfmt" "check"
      ]
      "docs" [
         "with-profile" "codox" "do" "codox"
      ]
   }


  :repositories  {"bintray"  "https://dl.bintray.com/content/narkisr/narkisr-jars"
                  "sonatype" "https://oss.sonatype.org/content/repositories/releases"
                  "libvirt-org" "https://libvirt.org/maven2"}

  :resource-paths  ["src/main/resources/"]

  :source-paths  ["src" "dev"]

  :target-path "target/"

  :test-paths  []

  :whidbey {
    :width 180
    :map-delimiter ""
    :extend-notation true
    :print-meta true
    :color-scheme {
      :delimiter [:blue]
       :tag [:bold :red]
    }
  }
  :repl-options {
    :init-ns user
    :prompt (fn [ns] (str "\u001B[35m[\u001B[34m" "re-core" "\u001B[35m]\u001B[33mλ:\u001B[m " ))
    :welcome (println "Welcome to re-core!" )
  }

)
