(defproject re-core "0.13.5"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/re-core-ops/re-core-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-config "0.2.0" ]

                 ; utils
                 [me.raynes/fs "1.4.6"]
                 [robert/hooke "1.3.0"]
                 [org.clojure/core.incubator "0.1.4"]
                 [slingshot "0.12.2" ]
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
                 [com.narkisr/gelfino-client "0.8.1"]
                 [com.taoensso/tufte "1.1.1"]

                 ; re-ops
                 [re-mote "0.2.3"]
                 [re-share "0.1.2"]

                 ;api
                 [clj-yaml "0.4.0"]
                 [org.clojure/data.json "0.2.6" ]

                 ; hypervisors
                 [narkisr/digitalocean "1.3"]
                 [potemkin "0.4.2"] ; see http://bit.ly/2mVr1sI
                 [amazonica "0.3.94" :exclusions [com.taoensso/nippy]]

                 ; libvirt
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [net.java.dev.jna/jna "4.2.0"]
                 [org.libvirt/libvirt "0.5.1"]

                 ; persistency and model
                 [com.brunobonacci/safely "0.2.4"]
                 [com.google.guava/guava "18.0"]
                 [clojurewerkz/elastisch "3.0.0-beta1"]
                 [puny "0.3.1"]
                 [com.taoensso/carmine "2.11.1"]
                 [commons-codec "1.10"]
                 [substantiation "0.3.1"]
                 [com.fasterxml.jackson.core/jackson-core "2.6.4"]

                 ; timeunits
                 [fogus/minderbinder "0.2.0"]

                 ; scheduling
                 [jarohen/chime "0.2.0" :exclusions [org.clojure/core.async]]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ; repl
                 [org.clojure/tools.nrepl "0.2.10"]
                 [io.aviso/pretty "0.1.33"]
                 [progrock "0.1.1"]

                 ; metrics
                 [metrics-clojure "2.9.0"]
                 [metrics-clojure-health "2.9.0"]
                 [metrics-clojure-jvm "2.9.0"]
               ]

  :exclusions [org.clojure/clojure com.taoensso/timbre commons-codec]

  :plugins  [[jonase/eastwood "0.2.4"]
              [mvxcvi/whidbey "1.3.1"]
             [lein-cljfmt "0.5.6"]
             [lein-kibit "0.1.5"]
             [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
             [lein-tar "2.0.0" ]
             [self-build "0.0.9"]
             [lein-tag "0.1.0"]
             [lein-set-version "0.3.0"]
             [topping "0.0.2"]]

  :bin {:name "re-core"}

  :profiles {
     :populate {
        :source-paths ["data"]
        :test-paths ["test"]
        :dependencies [[org.clojure/test.check "0.7.0"]]
     }


     :test {
       :test-paths ["test" "data"]
       :dependencies [
          [midje "1.8.3"]
          [org.clojure/tools.trace "0.7.9"]
          [org.clojure/test.check "0.7.0"]
        ]

        :jvm-opts ^:replace ["-Ddisable-conf=true"]
     }

     :dev {
        :source-paths  ["dev"]
        :resource-paths  ["src/main/resources/" "pkg/etc/re-core/"]
        :plugins [[lein-midje "3.1.3"]]
        :dependencies [[midje "1.8.3"]]

        :set-version {
           :updates [
             {:path "project.clj" :search-regex #"\"target\/re-core-\d+\.\d+\.\d+\.jar"}
             {:path "src/re-core/common.clj" :search-regex #"\"\d+\.\d+\.\d+\""}]}

      }
    }


  :jvm-opts ^:replace ["-Djava.library.path=/usr/lib:/usr/local/lib"]

  :aliases {
      "kvm"  ["with-profile" "test" "do" "midje" ":filter" "kvm"]
      "runtest" ["midje" ":filter" "-integration"]
      "populate" ["with-profile" "populate" "do" "run" "-m" "re-core.fixtures.populate"]
      "travis" [
        "with-profile" "test" "do"
        "midje" ":filter" "-integration," "midje" ":redis," "midje" ":elasticsearch,"
        "cljfmt" "check"
      ]
   }


  :repositories  {"bintray"  "http://dl.bintray.com/content/narkisr/narkisr-jars"
                  "sonatype" "http://oss.sonatype.org/content/repositories/releases"
                  "libvirt-org" "http://libvirt.org/maven2"}

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
