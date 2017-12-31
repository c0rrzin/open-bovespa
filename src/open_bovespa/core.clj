(ns open-bovespa.app
  (:require [com.stuartsierra.component :as component]
            [open-bovespa.impl.components.datomic :as datomic]
            [open-bovespa.impl.components.config :as config]
            [open-bovespa.impl.components.cvm :as cvm]
            [open-bovespa.protocols.cvm :as protocols.cvm]
            [open-bovespa.impl.adapters.cvm :as adapters.cvm]
            [open-bovespa.protocols.datomic :as protocols.datomic]))

(defn new-system []
  (component/system-map
    :config  (config/new-config)
    :cvm     (cvm/new-cvm-fetcher)
    :datomic (component/using (datomic/new-database)
                              [:config])))

(defn ensure-system-up! []
  (component/start-system (new-system)))

(defn fetch-adapt-and-save! []
  (let [sys (ensure-system-up!)]
    (doseq [company (->> (map :name (:all-companies (:config (:config sys))))
                         (filter #(> (compare % :marcopolo) 0)))]
      (let [entries
            (mapcat
              #(->> (protocols.cvm/fetch (:cvm sys) % company)
                    (adapters.cvm/all-entries company)
                    (datomic/enhance-entries))
              #{:assets :liabilities :detailed-earnings})]
        (println (str "Transacting " (count entries) " for " company))
        (protocols.datomic/transact! (:datomic sys) entries)))))



