(ns open-bovespa.impl.spec.financial-statement
  (:require [clojure.spec.alpha :as s]
            [open-bovespa.impl.spec.money :as money]))

(s/def ::quarter #{:q1 :q2 :q3 :q4})
(s/def ::year (s/and int? #(> % 1900)))

(s/def ::period-end inst?)
(s/def ::period-begin inst?)

(s/def ::share-price ::money/amount)
(s/def ::market-cap ::money/amount)
(s/def ::currency ::money/currency)

(s/def ::current-assets ::money/amount)
(s/def ::long-term-assets ::money/amount)
(s/def ::intangible-assets ::money/amount)
(s/def ::goodwill ::money/amount)
(s/def ::revenue ::money/amount)
(s/def ::cash-and-equivalents ::money/amount)
(s/def ::fixed-assets ::money/amount)

(s/def ::long-term-liabilities ::money/amount)
(s/def ::current-liabilities ::money/amount)
(s/def ::notes-payable ::money/amount)                      ; [bank] loans

(s/def ::earnings ::money/amount)
(s/def ::operating-income ::money/amount)
(s/def ::net-income ::money/amount)
(s/def ::net-profit-margin ::money/amount)

(s/def ::ordinary-shares ::money/amount)
(s/def ::preferred-shares ::money/amount)
(s/def ::shares-outstanding int?)
(s/def ::dividend-yield double?)
(s/def ::shareholders-equity ::money/amount)

(s/def ::yearly-exports ::money/amount)
(s/def ::yearly-imports ::money/amount)

(def assets #{::current-assets
              ::long-term-assets
              ::intangible-assets
              ::goodwill
              ::revenue
              ::cash-and-equivalents})

(def liabilities #{::long-term-liabilities
                   ::current-liabilities
                   ::notes-payable})

(def earnings #{::earnings
                ::operating-income
                ::net-income
                ::net-profit-margin})

(def book-accounts (->> (concat assets
                                liabilities
                                earnings)
                        (into #{})))

(defn build-financial-statement [es]
  (->> es
       (map (fn [e]
              [(::book-account e) (::amount e)]))))

(s/def ::entry (s/keys :req [::period-end ::amount ::currency ::book-account] :opt [::period-begin]))

