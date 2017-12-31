(ns open-bovespa.impl.investment
  (:require [open-bovespa.impl.spec.financial-statement :as financial-statement])
  (:import (java.util.concurrent TimeUnit)
           (java.math RoundingMode)))

(defn anualize-earning [{::financial-statement/keys [amount period-begin period-end]}]
  (if period-begin
    (let [start (.getTime period-begin)
          end (.getTime period-end)
          n (.convert TimeUnit/DAYS (- end start) TimeUnit/MILLISECONDS)]
      (bigdec (/ (* (double amount) 360) n)))
    amount))

(defn round [n]
  (.setScale n 2 RoundingMode/HALF_EVEN))

(defn build-financial-statement [eop es]
  (->> (filter #(= eop (::financial-statement/period-end %)) es)
       (map (fn [{::financial-statement/keys [book-account] :as fs}]
              [book-account (anualize-earning fs)]))
       (into {::financial-statement/period-end eop})))

(defn total-assets [{::financial-statement/keys [current-assets long-term-assets]}]
  (+ current-assets long-term-assets))

(defn total-liabilities [{::financial-statement/keys [current-liabilities long-term-liabilities]}]
  (+ current-liabilities long-term-liabilities))

(defn margin-of-profit [{::financial-statement/keys [operating-income revenue]}]
  (/ operating-income (double revenue)))

(defn debt-to-equity [{::financial-statement/keys [shareholders-equity] :as fs}]
  (-> (total-liabilities fs)
      (/ (double shareholders-equity))
      bigdec))

(defn net-current-asset-value [{::financial-statement/keys [current-assets preferred-shares] :as fs}]
  (- current-assets (+ (total-liabilities fs) preferred-shares)))
