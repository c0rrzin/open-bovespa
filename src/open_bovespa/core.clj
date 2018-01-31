(ns open-bovespa.app
  (:require [com.stuartsierra.component :as component]
            [open-bovespa.impl.components.datomic :as datomic]
            [open-bovespa.impl.components.config :as config]
            [open-bovespa.impl.components.cvm :as cvm]
            [open-bovespa.protocols.cvm :as protocols.cvm]
            [open-bovespa.impl.adapters.cvm :as adapters.cvm]
            [open-bovespa.impl.investment :as investment]
            [open-bovespa.protocols.datomic :as protocols.datomic]
            [open-bovespa.impl.spec.financial-statement :as financial-statement]
            [datomic.api :as d]))

(defn new-system []
  (component/system-map
    :config (config/new-config)
    :cvm (cvm/new-cvm-fetcher)
    :datomic (component/using (datomic/new-database) [:config])
    ))

(defn ensure-system-up! []
  (component/start-system (new-system)))

(defn fetch-adapt-and-save! []
  (try (let [sys (ensure-system-up!)]
         (doseq [company (dedupe (filter #(and (pos? (compare (name %) "tpi-triunfo-particip-invest"))
                                               (not= % :óleo-gás-participações)) (map :name (:all-companies (:config (:config sys))))))]
           #nu/tap company
           (let [entries (mapcat
                           #(->> (protocols.cvm/fetch (:cvm sys) % company)
                                 (adapters.cvm/all-entries company)
                                 #_financial-statement/build-financial-statement
                                 datomic/enhance-entries)
                           #{:assets :liabilities :detailed-earnings})]
             (println (str "Transacting " (count entries) " for " company))
             (protocols.datomic/transact! (:datomic sys) entries))))
       (catch Exception e #nu/tap e)))

(defn map-vals [f m]
  (->> m
       (map (fn [[k v]] [k (into {} (f v))]))))

(defn fetch-financial-statement []
  (let [sys (ensure-system-up!)]
    (->> name
         (d/q '{:find  [(pull $ ?e [*])]
                :in    [$]
                :where [[?e ::financial-statement/company _]]} (d/db (:datomic-connection (:datomic sys))))
         (flatten)
         (group-by ::financial-statement/company)
         (map-vals investment/build-financial-statement)
         (into {}))))

#_(def all-statements (fetch-financial-statement))

(def billion 1000000000M)

(defn relevant-company? [[_ {::financial-statement/keys [current-assets current-liabilities revenue long-term-liabilities] :as fs}]]
  (and (> (or revenue 0M) billion)
       (> current-assets (* 2 current-liabilities))
       (< (investment/debt-to-equity fs) 0.8M)))

(defn relevant-companies [statements]
  (filter relevant-company? statements))
