(ns re-core.clipboard
  "Basic clipboard reading support"
  (:import
   [java.awt.datatransfer DataFlavor StringSelection Transferable]))

(defn read-clipboard []
  (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
    (-> clipboard
        (.getContents nil)
        (.getTransferData (DataFlavor/stringFlavor)))))

(defn set-clipboard
  "Set clipboard, for example set last ip address:
     (set-clipboard (*ip)) "
  [s]
  (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
    (.setContents clipboard (StringSelection. s) nil)))
