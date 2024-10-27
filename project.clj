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
                 [re-share "0.18.0"]
                 [re-cog "0.6.7"]
                 [re-cipes "0.3.15"]
                 [re-scan "0.2.1"]

                 ; Elasticsearch
                 [rubber "0.4.1"]
                 [org.apache.httpcomponents/httpclient "4.5.13"]

                 ; XTDB
                 [com.xtdb/xtdb-core "1.19.0"]
                 [com.xtdb/xtdb-rocksdb "1.19.0"]

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
                 [org.clojure/core.match "1.0.0"]

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

  :gpg-verify {:deps [re-share
                      re-cog
                      re-cipes
                      rubber
                      narkisr/clj-yaml
                      ; 7083BA46F0B2460C
                      narkisr/digitalocean
                      substantiation

                      ; not signed?
                      ; narkisr/clansi


                      ; 115E8C72AE1DFBFDD4D8786BA56A26A672B08826
                      me.raynes/fs
                      me.raynes/conch

                      ; AFEDB040C1E8CE259F8B4B153DDA1B3EC890F586
                      com.palletops/stevedore

                      ; 0785B3EFF60B1B1BEA94E0BB7C25280EAE63EBE5
                      org.apache.httpcomponents/httpclient

                      ; FA7929F83AD44C4590F6CC6815C71C0A4E0B8EDD
                      net.java.dev.jna/jna

                      ; 1B933CE798B6985C8D79975DE1A36FB910E9E138
                      org.libvirt/libvirt

                      ; 6722D1BB1AFFC51AC43452E7161EC240CC48018B
                      less-awful-ssl

                      ; 6CA9E3B29F28FEA86750B6BE9D6465D43ACECAE0
                      cheshire

                      ; F55EF5BB19F52A250FEDC0DF39450183608E49D4
                      com.mikesamuel/json-sanitizer

                      ; 379CE192D401AB61
                      com.hierynomus/sshj

                      ; 174F88318B64CB02
                      org.zeromq/jeromq

                      ; F05B1D2EF3BED8C8E12F44EF6BEF76A7B805F61B
                      com.brunobonacci/safely

                      ; ABE9F3126BB741C1
                      com.google.guava/guava

                      ; 21939FF0CA2A6567
                      commons-codec

                      ; 148AA196DF8D6332
                      mount

                      ; 26406BB1AA04110E49AA8671A82090FF7CC19136
                      io.aviso/pretty

                      ; 8A10792983023D5D14C93B488D7F1BEC1E2ECAE7
                      com.fasterxml.jackson.core/jackson-core

                      ; AE1ABF3D484C40E5CB1CDC9F57B19548756012ED
                      com.draines/postal

                      ; 87FCFC781A1B513D
                      hiccup

                      ; 5CECAE951A380FC6C6982FE8361953C9DEB28012
                      riemann-clojure-client

                      ; 725F73F2BF6D0DEDAE758599DB3DCB7A484504A5
                      expound

                      ; D7EAC082D28BC79233279A3206CEE45EC93B3AF9
                      juxt/dirwatch

                      ; AF567B9777E77DDC
                      serializable-fn

                      ;  org.eclipse.aether.resolution.ArtifactResolutionException: Could not find artifact
                      ;; cc.qbits/knit
                      ;; com.rpl/specter
                      ;; fogus/minderbinder
                      ;; fipp

                      ; missing key 5D699B0B82216C222C641932BB3E754DDBBA7E53
                      ;; com.fzakaria/slf4j-timbre

                      ; missing key C84CF11DA459B1FC
                      ;; factual/durable-queue

                      ; missing key FF4A55A40618056ABF9B04BB97235BA29C3D58B7
                      ;; http-kit

                      ; missing key 76C0DF549B548A306D7D30578B012D9684A7F626
                      ;; com.cerner/clara-rules

                      ; missing key 7B1BE8B8F8A990BEB8AECD3B592525DD66E0BF75
                      ;; prismatic/schema

                      ; missing key 327D725B7F3AA97264F3643A2C2FDC653E12F5F0
                      ;; mvxcvi/puget

                      ; missing key FF4A55A40618056ABF9B04BB97235BA29C3D58B7
                      ;; com.taoensso/timbre
                      ;; com.taoensso/nippy

                      ; missing key C0AD8A1F364F03D61663D557CEFB17C8948BE7BB
                      ;; rm-hull/table

                      ; 8D06684A958AE602
                      org.clojure/test.check
                      org.clojure/data.codec
                      org.clojure/core.async
                      org.clojure/core.match
                      org.clojure/data.json
                      org.clojure/data.xml
                      org.clojure/data.zip
                      org.clojure/tools.namespace
                      org.clojure/core.incubator
                      org.clojure/java.classpath
                      org.clojure/clojure]}
   :profiles {
     :codox {
        :dependencies [[org.clojure/tools.reader "1.3.5"] [codox-theme-rdash "0.1.2"]] :plugins [[lein-codox "0.10.7"]]
        :codox {
            :project {:name "re-core"}
            :themes [:rdash]
            :source-paths ["src"]
            :source-uri "https://github.com/re-ops/re-core/blob/master/{filepath}#L{line}"
        }
     }

     :dev {
        :source-paths  ["src" "dev" "test"]
        :resource-paths  ["src/main/resources/"]
     }

     :build {
        :source-paths  ["dev" "test"]
        :set-version {
           :updates [
             {:path "project.clj" :search-regex #"\"target\/re-core-\d+\.\d+\.\d+\.jar"}
             {:path "src/re-core/common.clj" :search-regex #"\"\d+\.\d+\.\d+\""}]}
        :plugins [
             [lein-cljfmt "0.6.8"]
             [lein-ancient "0.7.0" :exclusions [org.clojure/clojure]]
             [lein-tag "0.1.0"]
             [lein-set-version "0.3.0"]
       ]
     }

     :verify {
        :plugins [[org.kipz/clj-gpg-verify "0.1.2"]]
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
        "with-profile" "build" "do" "unit," "integration," "cljfmt" "check"
      ]
      "docs" [
         "with-profile" "codox" "do" "codox"
      ]
   }

  :repositories  {
     "sonatype"    {:url "https://oss.sonatype.org/content/repositories/releases"}
     "libvirt-org" {:url "https://libvirt.org/maven2"}
  }

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
