(ns re-flow.disposable
  "Disposable flows"
  (:require
   [taoensso.timbre :refer (refer-timbre)]
   [clara.rules :refer :all]))

(refer-timbre)

(defrule diposeable-file-change
  "Check if a new file was created"
  [?e <- :re-flow.file-watcher/file [{:keys [role]}] (= role :diposable)]
  =>
  (info ?e))
