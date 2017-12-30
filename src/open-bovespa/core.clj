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

(defn fetch-adapt-and-save! [report company]
  (let [sys (ensure-system-up!)]
    (->> (protocols.cvm/fetch (:cvm sys) report company)
         (adapters.cvm/all-assets company)
         (datomic/enhance-entries)
         #_(protocols.datomic/transact! (:datomic sys)))))

