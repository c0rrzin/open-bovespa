(ns open-bovespa.impl.components.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [open-bovespa.impl.spec.financial-statement :as financial-statement]
            [open-bovespa.protocols.datomic :as protocols.datomic]))


(defn build-db-uri [{:keys [url db]}]
  (str "datomic:free://" url "/" db))

(defn connect [config]
  (d/connect (build-db-uri (get-in config [:config :datomic]))))

(def schema [{:db/ident       ::financial-statement/amount
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc         "Amount of entry"}

             {:db/ident       ::financial-statement/book-account
              :db/valueType   :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/doc         "Book account of entry"}

             {:db/ident       ::financial-statement/company
              :db/valueType   :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/doc         "Company of entry"}

             {:db/ident       ::financial-statement/currency
              :db/valueType   :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/doc         "Currency of entry"}

             {:db/ident       ::financial-statement/period-begin
              :db/valueType   :db.type/instant
              :db/cardinality :db.cardinality/one
              :db/doc         "Begin of period - may not be present"}

             {:db/ident       ::financial-statement/period-end
              :db/valueType   :db.type/instant
              :db/cardinality :db.cardinality/one
              :db/doc         "End of period"}

             {:db/ident       ::financial-statement/id
              :db/valueType   :db.type/uuid
              :db/unique      :db.unique/identity
              :db/cardinality :db.cardinality/one
              :db/doc         "ID of entry"}])

(defn enhance-entries [ds]
  (mapv (fn [d]
          (-> d
              (assoc :db/id (d/tempid :db.part/user))
              (assoc ::financial-statement/id (d/squuid)))) ds))

(defrecord Datomic [config]
  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    (let [conn (connect config)]
      (assoc component :datomic-connection conn)))

  (stop [component]
    (println ";; Stopping database")
    (assoc component :datomic-connection nil))

  protocols.datomic/Datomic
  (transact! [component data]
    (d/transact (:datomic-connection component) data))

  (transact-async! [component data]
    (d/transact-async (:datomic-connection component) data)))

(defn new-database
  ([config]
   (map->Datomic {:config config}))
  ([] (map->Datomic {})))