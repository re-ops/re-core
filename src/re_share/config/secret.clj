(ns re-share.config.secret
  "Secret file managment"
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [re-share.encryption :as enc :refer (read-pass)]))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [f]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream f) out)
    (.toByteArray out)))

(defn load-secrets
  "Load encrypted secrets file into target, this requires tmux (see read-pass)"
  [input target prv]
  (when-not (.exists (io/file target))
    (let [pass (clojure.string/trim (read-pass))
          output (enc/decrypt (slurp-bytes (io/file input)) prv pass)]
      (spit target output))))

(defn save-secrets
  "Save secrets.edn into an encrypted file"
  [input target pub]
  (let [input (slurp (io/file "/tmp/secrets.edn"))
        output (enc/encrypt input pub)]
    (with-open [out (io/output-stream (io/file target))]
      (.write out output))))
