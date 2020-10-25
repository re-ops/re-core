(defproject re-core "0.15.0"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/re-core-ops/re-core-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.10.1"]

                 ; utils
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/core.incubator "0.1.4"]

                 ; bash scripting
                 [com.palletops/stevedore "0.8.0-beta.7"]

                 ; logging / profiling
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/tufte "1.1.1"]
                 [timbre-ns-pattern-level "0.1.2"]
                 [com.fzakaria/slf4j-timbre "0.3.19"]
                 [org.clojure/tools.trace "0.7.9"]

                 ; re-ops
                 [re-share "0.16.7"]
                 [re-cog "0.5.13"]
                 [re-cipes "0.2.7"]
                 [re-scan "0.2.1"]

                 ; Elasticsearch
                 [rubber "0.4.1"]
                 [org.apache.httpcomponents/httpclient "4.5.2"]

                 ; Api
                 [clj-yaml "0.4.0"]

                 ; digitalocean
                 [narkisr/digitalocean "1.3"]

                 ; amazon
                 [amazonica "0.3.94" :exclusions [com.taoensso/nippy com.google.protobuf/protobuf-java]]

                 ; libvirt
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [net.java.dev.jna/jna "4.2.0"]
                 [org.libvirt/libvirt "0.5.1"]

                 ; lxc
                 [http-kit "2.3.0"]
                 [less-awful-ssl "1.0.4"]
                 [org.clojure/data.json "0.2.6" ]

                 ; model
                 [com.rpl/specter "1.1.2"]
                 [com.brunobonacci/safely "0.5.0"]
                 [com.google.guava/guava "18.0"]
                 [commons-codec "1.10"]
                 [substantiation "0.4.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.9.6"]

                 ; queue
                 [factual/durable-queue "0.1.5" :exclusions [byte-streams]]
                 [cc.qbits/knit "1.0.0"]

                 ; timeunits
                 [fogus/minderbinder "0.3.0"]

                 ; wiring
                 [mount "0.1.16"]

                 [me.raynes/conch "0.8.0"]
                 [org.clojure/core.async "1.2.603"]
                 [com.rpl/specter "1.1.2"]
                 [org.clojure/core.match "0.3.0-alpha4"]

                  ; persistency
                 [org.apache.httpcomponents/httpclient "4.5.2"]

                  ; pretty output
                 [fipp "0.6.14"]
                 [narkisr/clansi "1.2.0"]
                 [mvxcvi/puget "1.1.0"]
                 [rm-hull/table "0.7.0"]

                 ; pretty printing
                 [io.aviso/pretty "0.1.37"]

                 ; serialization
                 [serializable-fn "1.1.4"]
                 [org.clojure/data.codec "0.1.1"]
                 [com.taoensso/nippy "2.14.0"]
                 [cheshire "5.9.0"]
                 [com.mikesamuel/json-sanitizer "1.2.0"]

                 ; remote execution
                 [com.hierynomus/sshj "0.27.0" :exclusions [org.slf4j/slf4j-api]]
                 [org.zeromq/jeromq "0.5.1"]

                 ; model
                 [clj-time/clj-time "0.15.1"]

                 ; email
                 [com.draines/postal "2.0.3"]
                 [hiccup "1.0.5"]

                 ; monitoring
                 [riemann-clojure-client "0.5.0"]

                 ; spec
                 [expound "0.8.4"]
                 [org.clojure/test.check "0.10.0"]

                 ; rules
                 [com.cerner/clara-rules "0.20.0"]
                 [prismatic/schema "1.1.6"]
                 
                 ; folder watch
                 [juxt/dirwatch "0.2.5"]
               ]

  :exclusions [org.clojure/clojure com.taoensso/timbre commons-codec prismatic/schema]

  :plugins  [[lein-cljfmt "0.6.8"]
             [lein-ancient "0.6.15" :exclusions [org.clojure/clojure]]
             [self-build "0.0.9"]
             [lein-tag "0.1.0"]
             [lein-set-version "0.3.0"]]

  :profiles {
     :codox {
        :dependencies [[org.clojure/tools.reader "1.1.0"]
                       [codox-theme-rdash "0.1.2"]]
              :plugins [[lein-codox "0.10.7"]]
              :codox {:project {:name "re-core"}
                      :themes [:rdash]
                      :source-paths ["src"]
                      :source-uri "https://github.com/re-ops/re-core/blob/master/{filepath}#L{line}"
              }
     }

     :dev {
        :source-paths  ["dev" "test" "data"]
        :resource-paths  ["src/main/resources/"]
        :set-version {
           :updates [
             {:path "project.clj" :search-regex #"\"target\/re-core-\d+\.\d+\.\d+\.jar"}
             {:path "src/re-core/common.clj" :search-regex #"\"\d+\.\d+\.\d+\""}]}

     }
 }


  :jvm-opts ^:replace ["-Djava.library.path=/usr/lib:/usr/local/lib" "-Xms2g" "-Xmx2g"]

  :aliases {
      "unit" [
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
        "with-profile" "test" "do" "unit," "integration," "cljfmt" "check"
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

  :repl-options {
    :init-ns user
    :prompt (fn [ns]
              (let [hostname (.getHostName (java.net.InetAddress/getLocalHost))]
                (str "\u001B[35m[\u001B[34m" "re-core" "\u001B[31m" "@" "\u001B[36m" hostname "\u001B[35m]\u001B[33mλ:\u001B[m ")))
    :welcome (println "Welcome to re-core!" )
    :timeout 120000
  }

)
