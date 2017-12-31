(ns open-bovespa.protocols.datomic)

(defprotocol Datomic
  (transact! [component datoms] "Transacts entries to db")
  (transact-async! [component datoms] "Transacts entries to db. Returns a future"))

