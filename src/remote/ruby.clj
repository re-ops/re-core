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

(ns remote.ruby
  "A remoter that launches ruby script against an instance"
  (:require
    [clojure.core.strint :refer  (<<)]
    [slingshot.slingshot :refer [throw+]]
    [clojure.java.shell :refer (with-sh-dir)]
    [supernal.sshj :refer (copy sh- dest-path)]
    [celestial.common :refer (import-logging gen-uuid interpulate)]
    [me.raynes.fs :refer (delete-dir exists? mkdirs tmpdir)]
    [celestial.core :refer (Remoter)]
    [celestial.model :refer (rconstruct)])
  )

(import-logging)

(defrecord Ruby [src args dst timeout]
  Remoter
  (setup [this] 
         (when (exists? (dest-path src dst)) 
           (throw+ {:type ::old-code} "Old code found in place, cleanup first")) 
         (mkdirs dst) 
         (try 
           (sh- "ruby" "-v" {:dir dst})
           (catch Throwable e
             (throw+ {:type ::ruby-sanity-fail} "Failed to run ruby sanity step")))
         (copy src dst {}))
  (run [this]
       (info (dest-path src dst))
       (apply sh- "ruby" (conj args {:dir (dest-path src dst) :timeout timeout})))
  (cleanup [this]
           (delete-dir dst)))

(defmethod rconstruct :ruby [{:keys [src ruby name]:as action} 
                             {:keys [env] :as run-info}]
    (let [{:keys [args timeout]} (ruby env)]
      (->Ruby src (mapv #(interpulate % run-info) args) (<< "~(tmpdir)/~(gen-uuid)/~{name}") timeout)))
