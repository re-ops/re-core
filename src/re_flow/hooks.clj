(ns re-flow.hooks
  "Dynamic hooks loading system"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clara.rules.compiler :as compiler]
   [taoensso.timbre :refer (refer-timbre info debug warn error)]))

(refer-timbre)

(defn find-hook-namespaces
  "Dynamically find all Clojure namespaces in the hooks directory"
  []
  (let [hooks-dir (io/file "hooks")
        clj-files (when (.exists hooks-dir)
                    (filter #(and (.isFile %) (.endsWith (.getName %) ".clj")) 
                            (file-seq hooks-dir)))]
    (for [file clj-files
          :let [file-name (.getName file)
                base-name (str/replace file-name #"\.clj$" "")
                ;; Convert underscores to hyphens for namespace
                ;; Don't add hooks. prefix - use namespace as defined in the file
                ns-name (symbol (str/replace base-name "_" "-"))]]
      ns-name)))

(defn load-hook-namespaces
  "Load all hook namespaces found in the hooks directory"
  []
  (let [hook-namespaces (find-hook-namespaces)]
    (if (seq hook-namespaces)
      (do
        (info "Loading hooks from:" (str/join ", " (map name hook-namespaces)))
        (doseq [ns-sym hook-namespaces]
          (try
            (require ns-sym)
            (info "Loaded hook namespace:" ns-sym)
            (catch Exception e
              (error "Failed to load hook namespace:" ns-sym "Error:" (.getMessage e))))))
      (info "No hooks found in hooks directory"))))

(defn create-session-with-hooks
  "Create a new session that includes both core rules and any hooks"
  [sources options]
  (load-hook-namespaces)
  (let [hook-namespaces (find-hook-namespaces)
        all-sources (into sources hook-namespaces)]
    (info "Creating session with sources:" all-sources)
    (compiler/mk-session (flatten (into all-sources options)))))
