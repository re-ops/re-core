(comment 
  Celestial, Copyright 2013 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License. 
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0                     
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,                                      
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.) 

(ns celestial.ssl
  "SSL cert generation"
  (:import 
    java.util.Date
    java.io.FileOutputStream
    java.security.KeyStore
    java.security.PrivateKey
    java.security.cert.X509Certificate
    java.util.Date
    sun.security.x509.CertAndKeyGen
    sun.security.x509.X500Name)
  (:use 
    [celestial.common :only (get*)]
    [clojure.core.strint :only (<<)]))

(def keysize 1024) 
(def cname "celesital-ops.local") 
(def org-unit "IT")
(def org  "test")
(def city "TA")
(def state "IL") 
(def country "IL")
(def validity 1096)
(def alias- "celestial-ops-jetty")

(defn generate 
  "Generates a java keystore file with defined spec" 
  [output key-pass]
  (let [keystore (doto (KeyStore/getInstance "JKS") (.load nil nil)) 
        keypair (CertAndKeyGen. "RSA" "SHA1WithRSA" nil) 
        x500 (X500Name. cname org-unit org city state country) ]
    (.generate keypair keysize)
    (let [private-key (.getPrivateKey keypair)
          chain (.getSelfCertificate keypair x500 (Date.) (long (* validity 24 60 60)))]
      (.setKeyEntry keystore alias- private-key key-pass  (into-array X509Certificate [chain]))
      (.store keystore (FileOutputStream. output) key-pass))))


