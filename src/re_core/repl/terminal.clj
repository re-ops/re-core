(ns re-core.repl.terminal
  "Terminal manipulation"
  (:require
   [clojure.core.strint :refer  (<<)]
   [clojure.java.shell :refer (sh)]))

(defn launch-ssh [target private-key]
  (if (System/getenv "SSH_CONNECTION")
    (sh "tmux" "split-window" (<< "/usr/bin/ssh ~{target} -i ~{private-key}"))
    (sh "/usr/bin/x-terminal-emulator" "--disable-factory" "-e" (<< "/usr/bin/ssh ~{target} -i ~{private-key}"))))

