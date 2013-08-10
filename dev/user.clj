(ns user
  (:use midje.repl)
  (:require 
     [clojure.tools.trace :refer (deftrace trace trace-ns trace-vars)]
     [clojure.java.io :as io]
     [clojure.string :as str]
     [clojure.pprint :refer (pprint)]
     [clojure.repl :refer :all]
     [clojure.tools.namespace.repl :refer (refresh refresh-all)]
     [celestial.launch :as launch]))

