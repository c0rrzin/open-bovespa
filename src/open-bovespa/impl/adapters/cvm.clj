(ns open-bovespa.impl.adapters.cvm
  (:require [clojure.string :as string]
            [open-bovespa.impl.spec.financial-statement :as financial-statement]
            [clojure.instant :as inst]
            [clj-time.format :as time-format]))

(defn date-str->inst [s]
  (->> s
       (time-format/parse (time-format/formatter "dd/MM/yyyy"))
       (time-format/unparse (time-format/formatters :date))
       (inst/read-instant-date)))

(defn parse-eop-rule [company currency filter-fn book-account ms]
  (->> ms
       (filter filter-fn)
       (map (partial remove (fn [[k _]] (#{:Conta :Descrição} k))))
       first
       (map (fn [[k v]]
              {::financial-statement/period-end   (date-str->inst (string/replace (str k) #":" ""))
               ::financial-statement/amount       (* (bigdec v) 1000)
               ::financial-statement/company      company
               ::financial-statement/currency     currency
               ::financial-statement/book-account book-account}))))

(defn current-assets [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (or (re-find #"Ativo Circulante" (:Descrição m))
          (re-find #"Aplicações Financeiras" (:Descrição m))))
    ::financial-statement/current-assets
    ms))

(defn intangible-assets [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (and (re-find #"Intangível" (:Descrição m))
           (<= (count (:Conta m)) 5)))
    ::financial-statement/intangible-assets
    ms))

(defn long-term-assets [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (or (re-find #"Ativo Não Circulante" (:Descrição m))
          (re-find #"Ativo Realizável a Longo Prazo" (:Descrição m))))
    ::financial-statement/long-term-assets
    ms))

(defn goodwill-assets [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (re-find #"Goodwill" (:Descrição m)))
    ::financial-statement/goodwill
    ms))

(defn cash-and-equivalent-assets [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (and (re-find #"Caixa" (:Descrição m))
           (<= (count (:Conta m)) 3)))
    ::financial-statement/cash-and-equivalents
    ms))

(defn fixed-assets [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (and (re-find #"Imobilizado" (:Descrição m))
           (<= (count (:Conta m)) 5)))
    ::financial-statement/fixed-assets
    ms))

(defn all-assets [company ms]
  (concat (current-assets company ms)
          (long-term-assets company ms)
          (fixed-assets company ms)
          (intangible-assets company ms)
          (goodwill-assets company ms)
          (cash-and-equivalent-assets company ms)))

(defn current-liabilities [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (re-find #"Passivo Circulante" (:Descrição m)))
    ::financial-statement/current-liabilities
    ms))

(defn long-term-liabilities [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (re-find #"Passivo Não Circulante") (:Descrição m))
    ::financial-statement/long-term-liabilities
    ms))

(defn shareholders-equity [company ms]
  (parse-eop-rule
    company
    :BRL
    (fn [m]
      (and (re-find #"Patrimônio" (:Descrição m))
           (<= (count (:Conta m)) 3)))
    ::financial-statement/long-term-liabilities
    ms))

(defn all-liabilities [company ms]
  (concat (current-liabilities company ms)
          (long-term-liabilities company ms)
          (shareholders-equity company ms)))