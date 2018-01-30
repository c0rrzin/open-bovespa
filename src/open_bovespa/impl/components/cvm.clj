(ns open-bovespa.impl.components.cvm
  (:require [clj-http.client :as client]
            [hickory.core :as html]
            [hickory.select :as s]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [open-bovespa.protocols.cvm :as protocols.cvm]
            [com.stuartsierra.component :as component]))

(def query-params-boiler {"Grupo"                       "DF"
                          "Informacao"                  "2"
                          "Periodo"                     "0"
                          "Quadro"                      "DF"
                          "NomeTipoDocumento"           "DFP"
                          "Titulo"                      "DF"
                          "DataReferencia"              "31/12/2017"
                          "Versao"                      "1"
                          "Empresa"                     "Company"
                          "CodTipoDocumento"            "4"
                          "CodigoTipoInstituicao"       "2"
                          "NumeroSequencialRegistroCvm" "1617"})

(def start-url "https://www.rad.cvm.gov.br/ENETCONSULTA/frmGerenciaPaginaFRE.aspx?NumeroSequencialDocumento=69302&CodigoTipoInstituicao=2")

(def headers {"DNT" "1"
              "Accept-Encoding" "gzip, deflate, br"
              "Accept-Language" "en-US,en;q=0.9,pt-BR;q=0.8,pt;q=0.7"
              "Upgrade-Insecure-Requests" "1"
              "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
              "Cookie" "ASP.NET_SessionId=wjuq5y55sq22fda0jzztbl45; BIGipServerpool_www.rad.cvm.gov.br_443=1225266186.47873.0000; TS01871345=016e3b076fbed2834a7d59b0c5bc7d6cb30a7956f606186c8f8a27f8ee30c6ec63674b25023c0cbb0b996fecde2c19487114a89e97c11bf89d4683ef731188707a2030d855"})

(def config (read-string (slurp "resources/config.edn")))

(def reports #{:assets :liabilities :detailed-earnings :big-picture-earnings :cash-flow :pl-mutations})

(defn report->query-param [report] {"Demonstracao" (case report
                                                     :assets 2
                                                     :liabilities 3
                                                     :detailed-earnings 4
                                                     :big-picture-earnings 5
                                                     :pl-mutations 8
                                                     :added-value 9
                                                     :cash-flow 99)})

(defn sequential->num-seq-documento [sequential] {"NumeroSequencialDocumento" sequential})

(defn fetch-report-sequential [config company]
  (let [bov-session " _ga=GA1.4.1130901207.1448492351; _ga=GA1.3.847670538.1514052812; idioma=pt-br; _gid=GA1.4.2023810036.1514246698; ASPSESSIONIDSCQQAAAT=AFOPCPCCABDMCFEOMAJJOPBD; TabSelecionada=2; ASPSESSIONIDQQCTCCSD=EGBECPCCAFKPIAJPCFEKKHJN; _gid=GA1.3.1030552667.1514469552; ASPSESSIONIDQQSBBCDA=PCFMBPCCEBPFPAFFDCPEOOCO; ASPSESSIONIDQCBQRRCR=JIEKDEECLHCGKOBHBGEHIFLJ; TS01871345=016e3b076f2dfa0283befb097940ca2b23fa6aed49aa33c593776bc73797e2d3df246aa44769ca6d33f31e6286115de8a26ad56e97"
        company-map (first (filter #(= (:name %) company) (:all-companies config)))]
    (-> (client/get "http://bvmf.bmfbovespa.com.br/cias-listadas/empresas-listadas/ResumoDemonstrativosFinanceiros.aspx"
                 {:headers      (assoc headers "Cookie" bov-session)
                  :query-params (merge query-params-boiler
                                       {"codigoCvm" (:cvm-code company-map)})})
        :body
        (->> (re-find #"NumeroSequencialDocumento=\d+"))
        (string/split #"=")
        second)))

(defn fetch-report [session report company]
  (let [report-sequential (fetch-report-sequential config company)]
    (client/get "https://www.rad.cvm.gov.br/ENETCONSULTA/frmDemonstracaoFinanceiraITR.aspx"
                {:headers      headers
                 :cookies      session
                 :query-params (merge query-params-boiler
                                      (report->query-param report)
                                      (sequential->num-seq-documento report-sequential))})))

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (mapv #(keyword (string/replace % #" "  "")))
            repeat)
       (mapv #(mapv (fn [x] (-> x
                                (string/replace #" " "")
                                (string/replace #"\." ""))) %) (rest csv-data))))

(defn pre-parse [body]
  (-> body
      (string/replace #"<br>" "")
      (string/replace #"<br>" "")
      (string/replace #"&nbsp;" "")))

(defn parse-report [report]
  (->>
   (walk/postwalk
     (fn [tag]
       (cond

         (= (count (:content tag)) 1)
         (first (:content tag))

         (and (pos? (count (:content tag))) (every? string? (:content tag)))
         (:content tag)

         :else tag))
     (s/select
       (s/id :ctl00_cphPopUp_tbDados)
       (-> report
           :body
           pre-parse
           html/parse
           html/as-hickory)))
   first :content second :content
   (mapv (fn [content] (remove #{\return \tab \newline "\r\n\t\t" "\r\n" "\r\n\t"} content)))
   (remove empty?)))

(defn big-picture-entries [es]
  (filterv (fn [e] (<= (count (:Conta e)) 3)) es))

(defn filter-maps [ms]
  (->> ms
       (remove (fn [{:keys [Conta]}] (= Conta "[:type :element]")))
       vec))

(defrecord CVMFetcher []

  component/Lifecycle
  (start [component]
    (println ";; Starting CVMFetcher")
    (let [cookies (:cookies (client/get start-url))]
      (assoc component :cvm-session cookies)))

  (stop [component]
    (println ";; Stopping CVMFetcher")
    (assoc component :cvm-session nil))

  protocols.cvm/CVMFetcher
  (fetch [component report company]
    (->> (fetch-report (:cvm-session component) report company)
         parse-report
         csv-data->maps
         filter-maps))

  (refresh [component]
    (let [cookies (:cookies (client/get start-url))]
      (assoc component :cvm-session cookies))))

(defn new-cvm-fetcher []
  (map->CVMFetcher {}))

;; FETCH ALL COMPANIES
#_ (do
     (defn to-company-maps [s]
       (-> s
           (string/replace #"\[\"" "")
           (string/replace #"\"]" "")
           (string/replace #"\." "")
           (string/replace #"/" "")
           (string/lower-case)
           (string/replace #" e " " ")
           (string/replace #" sa" "")
           (string/replace #" part " " ")
           (string/replace #" " "-")
           (string/split #"\*")
           (->> (zipmap [:name :cvm-code]))))

     (->>
       (walk/postwalk
         (fn [tag]
           (cond
             (and (get-in tag [:attrs :href])
                  (string? (get-in tag [:attrs :href]))
                  (get-in tag [:content])
                  (string? (get-in tag [:attrs :href])))
             (str (get-in tag [:content]) "*" (string/replace (get-in tag [:attrs :href]) #"ResumoEmpresaPrincipal\.aspx\?codigoCvm=" ""))

             (= (count (:content tag)) 1)
             (first (:content tag))

             (and (pos? (count (:content tag))) (every? string? (:content tag)))
             (:content tag)

             :else tag))
         (nu/tap (s/select (s/id :ctl00_contentPlaceHolderConteudo_BuscaNomeEmpresa1_grdEmpresa_ctl01) (html/as-hickory (html/parse allcompanies)))))
       first :content butlast last :content
       (map second)
       (remove #{\tab})
       (map to-company-maps)
       (mapv #(update % :name keyword))))
