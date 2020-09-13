(ns re-core.clipboard
  "Basic clipboard reading support"
  (:require
   [re-share.core :refer (error-m)])
  (:import
   [java.awt.datatransfer DataFlavor StringSelection Transferable]))

(defn read-clipboard []
  (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
    (-> clipboard
        (.getContents nil)
        (.getTransferData (DataFlavor/stringFlavor)))))

(defn set-clipboard [s]
  (let [clipboard (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit))]
    (.setContents clipboard (StringSelection. s) nil)))
