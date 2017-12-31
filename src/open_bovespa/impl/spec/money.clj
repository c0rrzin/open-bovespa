(ns open-bovespa.impl.spec.money
  (:require [clojure.spec.alpha :as s]))

(def bigdec? #(= (bigdec %) %))

(s/def ::amount (s/and bigdec? pos?))

(s/def ::simple-amount bigdec?)

(s/def ::currency #{:BRL :USD})