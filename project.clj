(defproject re-core "0.17.0"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/re-core-ops/re-core-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.10.3"]

                 [org.clojure/java.classpath "1.0.0"]

                 ; utils
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/core.incubator "0.1.4"]

                 ; bash scripting
                 [com.palletops/stevedore "0.8.0-beta.7"]

                 ; logging / profiling
                 [com.taoensso/timbre "5.1.2"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]

                 ; re-ops
                 [re-share "0.17.2"]
                 [re-cog "0.6.5"]
                 [re-cipes "0.3.12"]
                 [re-scan "0.2.1"]

                 ; Elasticsearch
                 [rubber "0.4.1"]
                 [org.apache.httpcomponents/httpclient "4.5.13"]

                 ; repl
                 [org.clojure/tools.namespace "1.1.0"]

                 ; Api
                 [narkisr/clj-yaml "0.7.2"]

                 ; digitalocean
                 [narkisr/digitalocean "1.3"]

                 ; libvirt
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "1.0.0"]
                 [net.java.dev.jna/jna "5.6.0"]
                 [org.libvirt/libvirt "0.5.2"]

                 ; lxc
                 [http-kit "2.5.3"]
                 [less-awful-ssl "1.0.6"]
                 [org.clojure/data.json "1.0.0" ]

                 ; model
                 [com.rpl/specter "1.1.3"]
                 [com.brunobonacci/safely "0.5.0"]
                 [com.google.guava/guava "23.0"]
                 [commons-codec "1.15"]
                 [substantiation "0.4.0"]
                 [com.fasterxml.jackson.core/jackson-core "2.12.2"]

                 ; queue
                 [factual/durable-queue "0.1.6" :exclusions [byte-streams]]
                 [cc.qbits/knit "1.0.0"]

                 ; timeunits
                 [fogus/minderbinder "0.3.0"]

                 ; wiring
                 [mount "0.1.16"]

                 [me.raynes/conch "0.8.0"]
                 [org.clojure/core.async "1.3.618"]
                 [com.rpl/specter "1.1.3"]
                 [org.clojure/core.match "1.0.0"]

                  ; persistency
                 [org.apache.httpcomponents/httpclient "4.5.13"]

                  ; pretty output
                 [fipp "0.6.23"]
                 [narkisr/clansi "1.2.0"]
                 [mvxcvi/puget "1.3.1"]
                 [rm-hull/table "0.7.1"]

                 ; pretty printing
                 [io.aviso/pretty "0.1.37"]

                 ; serialization
                 [serializable-fn "1.1.4"]
                 [org.clojure/data.codec "0.1.1"]
                 [com.taoensso/nippy "2.14.0"]
                 [cheshire "5.10.0"]
                 [com.mikesamuel/json-sanitizer "1.2.2"]

                 ; remote execution
                 [com.hierynomus/sshj "0.31.0" :exclusions [org.slf4j/slf4j-api]]
                 [org.zeromq/jeromq "0.5.2"]

                 ; model
                 [clj-time/clj-time "0.15.2"]

                 ; email
                 [com.draines/postal "2.0.4"]
                 [hiccup "1.0.5"]

                 ; monitoring
                 [riemann-clojure-client "0.5.1"]

                 ; spec
                 [expound "0.8.9"]
                 [org.clojure/test.check "1.1.0"]

                 ; rules
                 [com.cerner/clara-rules "0.21.0"]
                 [prismatic/schema "1.1.12"]

                 ; folder watch
                 [juxt/dirwatch "0.2.5"]
               ]

  :exclusions [org.clojure/clojure com.taoensso/timbre commons-codec prismatic/schema]

  :plugins  [[lein-cljfmt "0.6.8"]
             [lein-ancient "0.7.0" :exclusions [org.clojure/clojure]]
             [lein-tag "0.1.0"]
             [lein-set-version "0.3.0"]]

  :profiles {
     :codox {
        :dependencies [[org.clojure/tools.reader "1.3.5"]
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

  :repositories  {"sonatype" "https://oss.sonatype.org/content/repositories/releases"
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
