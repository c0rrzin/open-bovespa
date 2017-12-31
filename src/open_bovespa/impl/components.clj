(ns open-bovespa.impl.components
  (:require [com.stuartsierra.component :as component]
            [open-bovespa.impl.components.config :as config]
            [open-bovespa.impl.components.cvm :as cvm]
            [open-bovespa.impl.components.datomic :as datomic]))

(defn example-system [config-options]
  (let [{:keys [host port]} config-options]
    (component/system-map
      :config (config/new-config)
      :datomic (component/using datomic/new-database [:config])
      :scheduler (new-scheduler)
      :app (component/using
             (example-component config-options)
             {:database  :db
              :scheduler :scheduler}))))

