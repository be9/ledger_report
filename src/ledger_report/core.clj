(ns ledger-report.core
  (:require [clojure.tools.cli               :refer [parse-opts]]
            [ledger-report.commands.cashflow :refer [cashflow]]
            [clojure.java.io                 :as io]
            [clojure.edn                     :as edn]
            [clojurewerkz.money.currencies   :as mc])
  (:gen-class))

(def cli-options
  [["-f" "--file=FILE"   "Файл ledger"             :default "my.ldg"]
   ["-m" "--meta=FILE"   "Файл с описанием счетов" :default "accounts.edn"]
   ["-p" "--prices=FILE" "Файл с котировками"]])

(defn read-edn
  "Открывает и читает файл данных filename в формате EDN. Возвращает прочитанную
   структуру данных или nil."
  [filename]
  (try
    (with-open [infile (java.io.PushbackReader. (io/reader filename))]
      (edn/read infile))
    (catch java.io.FileNotFoundException e
      nil)))

(def empty-metadata
  {
    :earnings {}
    :payments {}
  })

(defn global-config
  "Map с глобальной конфигурацией"
  [opts]
  (let [options   (:options opts)
        metadata  (read-edn (:meta options))]

    (assoc options :metadata (or metadata empty-metadata)
                   :currency mc/RUB)))

(defn -main
  [& args]
  (let [opts      (parse-opts args cli-options :in-order true)
        arguments (:arguments opts)
        cmd       (first arguments)]
    (cond
      (= cmd "cashflow") (cashflow (global-config opts) arguments)
      :else              (println "Unknown command. Available commands: cashflow\n"
                                  (:summary opts))))
  (System/exit 0))
