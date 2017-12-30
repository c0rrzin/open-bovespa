(ns open-bovespa.impl.investment
  (:require [open-bovespa.impl.spec.financial-statement :as financial-statement]))

(defn total-assets [{::financial-statement/keys [current-assets long-term-assets]}]
  (+ current-assets long-term-assets))

(defn total-liabilities [{::financial-statement/keys [current-liabilities long-term-liabilities]}]
  (+ current-liabilities long-term-liabilities))

(defn margin-of-profit [{::financial-statement/keys [operating-income revenue]}]
  (/ operating-income revenue))

(defn debt-to-equity [{::financial-statement/keys [shareholders-equity] :as fs}]
  (-> (total-liabilities fs)
      (/ shareholders-equity)))

(defn net-current-asset-value [{::financial-statement/keys [current-assets preferred-shares] :as fs}]
  (- current-assets (+ (total-liabilities fs) preferred-shares)))
