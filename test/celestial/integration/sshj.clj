(ns celestial.integration.sshj
  "Basic sshj functionlity"
  (:use 
    midje.sweet
    [clojure.java.io :only (file)]
    [supernal.sshj :only (copy execute sh-)]))


(def remote {:host "192.168.5.9" :user "vagrant"})
(def git-uri "git://github.com/narkisr/cap-demo.git")
(def http-uri "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox-0.3.4.tar.gz")

(fact "git remote clone" :integration :sshj
      (execute "rm -rf /tmp/cap-demo" remote)
      (copy git-uri "/tmp" remote) => nil)

(fact "remote http get" :integration :sshj
      (execute "rm /tmp/redis-sandbox-0.3.4.tar.gz" remote)
      (copy http-uri "/tmp" remote) => nil)

(fact "local file copy to remote" :integration :sshj
      (execute "rm /tmp/project.clj" remote)
      (copy "project.clj" "/tmp" remote) => nil)

(fact "git local clone"
      (sh- "rm" "-rf" "/tmp/cap-demo") 
      (copy "git://github.com/narkisr/cap-demo.git" "/tmp")
      (.exists (file "/tmp/cap-demo")) => truthy)

(fact "local file copy to local" :integration :sshj
      (copy "project.clj" "/tmp") => nil
      (.exists (file "/tmp/project.clj")) => truthy
      (sh- "rm" "/tmp/project.clj"))

