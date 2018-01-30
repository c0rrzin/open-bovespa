(ns open-bovespa.app
  (:require [com.stuartsierra.component :as component]
            [open-bovespa.impl.components.datomic :as datomic]
            [open-bovespa.impl.components.config :as config]
            [open-bovespa.impl.components.cvm :as cvm]
            [open-bovespa.protocols.cvm :as protocols.cvm]
            [open-bovespa.impl.adapters.cvm :as adapters.cvm]
            [open-bovespa.protocols.datomic :as protocols.datomic]
            [open-bovespa.impl.spec.financial-statement :as financial-statement]))

(defn new-system []
  (component/system-map
    :config (config/new-config)
    :cvm (cvm/new-cvm-fetcher)
    ;:datomic (component/using (datomic/new-database) [:config])
    ))

(defn ensure-system-up! []
  (component/start-system (new-system)))

(defn fetch-adapt-and-save! [c]
  (let [sys (ensure-system-up!)]
    (let [entries
          (mapcat
            #(->> (protocols.cvm/fetch (:cvm sys) % c)
                  (adapters.cvm/all-entries c)
                  financial-statement/build-financial-statement)
            #{:assets :liabilities :detailed-earnings})]
      (into {} entries)
      #_(println (str "Transacting " (count entries) " for " company))
      #_(protocols.datomic/transact! (:datomic sys) entries))))



