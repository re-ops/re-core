(comment 
   re-core, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns components.core
  "A less intrusive component model"
 )

(defprotocol Lifecyle
  (setup [this] "A one time setup (per application run) code") 
  (start [this] "Start this component")
  (stop  [this] "Stop this component"))

(defn start-all 
   "start all components" 
   [cs]
   (doseq [[k c] cs] (.start c)))

(defn stop-all 
   "stops all components" 
   [cs]
   (doseq [[k c] cs] (.stop c)))

(defn setup-all 
   "setup all components" 
   [cs]
   (doseq [[k c] cs] (.setup c)))
