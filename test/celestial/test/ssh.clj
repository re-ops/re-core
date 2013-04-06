(ns celestial.test.ssh
  (:import com.jcraft.jsch.Channel)
  (:use 
    midje.sweet
    clojure.core.strint
    [clj-ssh.ssh :only (add-identity with-connection connect)]
    [celestial.ssh :only (execute put copy run log-output step)]))

(fact "non seq batches" 
  (execute {:host "bla"} 1) => (throws java.lang.AssertionError))

#_(defn run* [code session batch] {:channel 
  (erajure.core/mock Channel (behavior (.getExitStatus) (int code))) })


#_(with-redefs [add-identity (fn [x y]) connect identity log-output identity run (partial run* 0)]
  (fact "batch execution triggers muliple runs"
    (execute {:host "bla"} (step :ls "cd /tmp" "ls")) => nil 
    (provided 
      (run ..anything.. "cd /tmp") => nil
      (run ..anything.. "ls") => nil))) 

(fact "copy local file results in local file put"
  (copy "localhost" "file://home/ronen/redis-sandbox.tar.gz" "/tmp") => nil
  (provided 
    (put "localhost" "/home/ronen/redis-sandbox.tar.gz" "/tmp") => nil :times 1))


(let [uri "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox.tar.gz"
      remote {:host "localhost"} dest "/tmp"]
  (fact "http results with wget execute"
    (copy remote uri dest) => nil
    (provided 
      (execute remote [(<< "wget -O /tmp/redis-sandbox.tar.gz ~{uri}")]) => nil :times 1)))


(let [uri "git://github.com/narkisr/celestial.git"
      remote {:host "localhost"} dest "/tmp"]
  (fact "git url results with execute clone"
     (copy remote uri dest) => nil
     (provided 
       (execute remote [(<< "git clone ~{uri} ~{dest}/celestial")]) => nil :times 1)))
