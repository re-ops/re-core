(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns remote.capistrano
  (:require
    [clojure.core.strint :refer  (<<)]
    [slingshot.slingshot :refer [throw+]]
    [clojure.java.shell :refer (with-sh-dir)]
    [supernal.sshj :refer (copy sh- dest-path)]
    [celestial.common :refer (import-logging gen-uuid interpulate)]
    [clojure.java.shell :refer [sh]]
    [me.raynes.fs :refer (delete-dir exists? mkdirs tmpdir)]
    [trammel.core :refer  (defconstrainedrecord)]
    [celestial.core :refer (Remoter)]
    [celestial.model :refer (rconstruct)]))

(import-logging)

(defconstrainedrecord Capistrano [src args dst]
  "A capistrano remote agent"
  []
  Remoter
  (setup [this] 
         (when (exists? (dest-path src dst)) 
           (throw+ {:type ::old-code :message "Old code found in place, cleanup first"})) 
         (mkdirs dst) 
         (try 
           (sh- "cap" "-T" {:dir dst})
           (catch Throwable e
             (error e)
             (throw+ {:type ::cap-not-found :message "Capistrano binary not found in path"})))
         (copy src dst))
  (run [this]
       (info (dest-path src dst))
       (apply sh- "cap" (conj args {:dir (dest-path src dst)})))
  (cleanup [this]
           (delete-dir dst)))

(defmethod rconstruct :capistrano [{:keys [src capistrano name] :as spec} run-info]
    (->Capistrano src (mapv #(interpulate % run-info) (capistrano :args)) (<< "~(tmpdir)/~(gen-uuid)/~{name}")))


