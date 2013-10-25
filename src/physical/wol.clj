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

(ns physical.wol
  "Wake on lan"
  (:import 
    (java.net DatagramSocket DatagramPacket InetAddress))
  (:require [clojure.string :refer (split)]))

(defn- ++
  "Combines two byte arrays to one"
  [^"[B" f ^"[B" s]
  (let [f-l (alength f) s-l (alength s)
        res (byte-array (+ f-l s-l))]
    (System/arraycopy f 0 res 0 f-l)
    (System/arraycopy s 0 res f-l s-l)
    res
    ))
(defn mac-bytes 
  "convert mac into byte array" 
  [mac]
  {:pre [(= 6 (count (split mac #"\:|\-")))]}
  (bytes (byte-array 
    (map #(unchecked-byte (Integer/parseInt % 16)) (split mac #"\:")))))

(defn payload [mac]
  (let [bs (mac-bytes mac) rep-bs (reduce ++ (byte-array 0) (repeat 16 bs))]
   (byte-array (concat (repeat 6 (unchecked-byte 0xff)) rep-bs))))

(defn wol [{:keys [mac broadcast]}]
  (let [bs (payload mac) ]
    (.send (DatagramSocket.)
           (DatagramPacket. bs (alength bs) (InetAddress/getByName broadcast) 9))))

;; (payload "6c:f0:49:e3:2a:4b")
;; (wol "00:24:8c:43:f3:f9" "192.168.5.255")
