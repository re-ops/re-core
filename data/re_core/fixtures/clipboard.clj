(ns re-core.fixtures.clipboard
  "clipboard utilities"
  (:import
   java.awt.datatransfer.StringSelection
   java.awt.Toolkit))

(defn clipboard-copy [s]
  (let [clp (.getSystemClipboard (Toolkit/getDefaultToolkit))]
    (.setContents clp (StringSelection. s) nil)))

;; (clipboard-copy (clojure.data.json/write-str redis-type :escape-slash false))
;; (clipboard-copy (clojure.data.json/write-str redis-vc-spec :escape-slash false))

