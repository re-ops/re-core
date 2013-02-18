(ns celestial.test.ssh
  (:use 
    clojure.core.strint
    expectations.scenarios  
    [celestial.ssh :only (execute put copy)]))

(scenario 
  (expect java.lang.AssertionError (execute {:host "bla"} "one two")))

(scenario
  (copy "localhost" "file://home/ronen/redis-sandbox.tar.gz" "/tmp")
  (expect (interaction (put "localhost" "/home/ronen/redis-sandbox.tar.gz" "/tmp"))))

(scenario
  (let [uri "http://dl.bintray.com/content/narkisr/boxes/redis-sandbox.tar.gz"
        host "localhost" dest "/tmp"]
    (copy host uri dest)
    (expect 
      (interaction (execute {:host host} [(<< "wget -O /tmp/redis-sandbox.tar.gz ~{uri}")])))))

(scenario
  (let [uri "git://github.com/narkisr/celestial.git"
        host "localhost" dest "/tmp"]
    (copy host uri dest)
    (expect 
      (interaction (execute {:host host} [(<< "git clone ~{uri} ~{dest}/celestial")])))))
