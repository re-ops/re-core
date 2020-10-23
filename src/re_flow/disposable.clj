(ns re-flow.disposable
  "Disposable flows"
  (:require
   [re-core.dispoable :refer (open-single)]
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(def supported-extensions #{".html" ".pdf" ".docx" ".doc" ".odt"})

(defrule diposeable-file-change
  "Check if a new file was created"
  [?e <- :re-flow.file-watcher/file
   [{:keys [role action extension]}] (= role :disposable) (= action :modify) (supported-extensions extension)]
  =>
  (info "opening file" (?e :file))
  (open-single (.getPath (?e :file))))
