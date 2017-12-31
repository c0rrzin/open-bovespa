(ns open-bovespa.impl.spec.company
  (:require [clojure.spec.alpha :as s]))

(s/def ::name string?)

(s/def ::country #{:brazil :usa :germany})

(s/def ::industries #{:oil-and-gas :technology :finance :automotive
                      :paper-and-celulose :extraction-and-mineration :agrobusiness
                      :real-state :all})

(s/def ::sector #{:industrials})

(s/def ::CEO string?)

(s/def ::founding-date inst?)
