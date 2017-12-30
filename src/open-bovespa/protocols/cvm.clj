(ns open-bovespa.protocols.cvm)

(defprotocol CVMFetcher
  (fetch [component report company] "Fetch report from CVM")
  (refresh [component] "Refresh session"))
