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

(ns kvm.networking
  (:require 
    [clojure.java.shell :refer [sh]]
    [clojure.data.zip.xml :as zx]
    [kvm.common :refer (connect domain-zip)]))

(def connection (connect "qemu+ssh://ronen@localhost/system"))

(defn macs [c id]
  (let [root (domain-zip c id)]
    (map vector (zx/xml-> root :devices))))

(macs connection "ubuntu-15.04")

(let [{:keys [out exit]} (sh "/usr/sbin/arp" "-an")]
  (filter #(.contains % "52:54:00:49:bc:85") (.split out "\\n" )) 
  )

(.getDHCPLeases (.networkLookupByName connection "default"))
