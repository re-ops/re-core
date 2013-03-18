(ns celestial.test.ssh
  (:import com.jcraft.jsch.Channel)
  (:use 
    clojure.core.strint
    expectations.scenarios  
    [erajure.core :only (mock behavior)]  
    [clj-ssh.ssh :only (add-identity with-connection connect)]
    [celestial.ssh :only (execute put copy run log-output step)]))

(scenario 
  (expect java.lang.AssertionError (execute {:host "bla"} 1)))


(defn run* [code session batch] {:channel 
  (mock Channel (behavior (.getExitStatus) (int code))) })

(scenario
  (with-redefs [add-identity (fn [x y]) connect identity 
                log-output identity run (partial run* 0)]
    (execute {:host "bla"} (step :ls "cd /tmp" "ls")))
  (interaction (run anything "cd /tmp"))
  (interaction (run anything "ls")))

(scenario
  (copy "localhost" "file://home/ronen/redis-sandbox.tar.gz" "/tmp")
  (expect (interaction (put "localhost" "/home/ronen/redis-sandbox.tar.gz" "/tmp"))))

(scenario
  (let [uri "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox.tar.gz"
        remote {:host "localhost"} dest "/tmp"]
    (copy remote uri dest)
    (expect 
      (interaction (execute remote [(<< "wget -O /tmp/redis-sandbox.tar.gz ~{uri}")])))))

(scenario
  (let [uri "git://github.com/narkisr/celestial.git"
        remote {:host "localhost"} dest "/tmp"]
    (copy remote uri dest)
    (expect 
      (interaction (execute remote [(<< "git clone ~{uri} ~{dest}/celestial")])))))
