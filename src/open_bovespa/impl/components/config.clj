(ns open-bovespa.impl.components.config
  (:require [com.stuartsierra.component :as component]))

(defn read-config []
  (read-string
    (slurp "resources/config.edn")))

(defrecord Config []

  component/Lifecycle
  (start [component]
    (println ";; Starting config")
    (assoc component :config (read-config)))

  (stop [component]
    (println ";; Stoping config")
    (assoc component :config nil)))

(defn new-config []
  (map->Config {}))
