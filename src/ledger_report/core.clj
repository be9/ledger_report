(ns ledger-report.core
  (:require [clojure.tools.cli               :refer [parse-opts]]
            [ledger-report.commands.cashflow :refer [cashflow]]
            [ledger-report.commands.budget   :refer [budget]]
            [ledger-report.tools             :refer :all]
            [clojurewerkz.money.currencies   :as mc])
  (:gen-class))

(def cli-options
  [["-f" "--file=FILE"   "Файл ledger"             :default "my.ldg"]
   ["-m" "--meta=FILE"   "Файл с описанием счетов" :default "accounts.edn"]
   ["-p" "--prices=FILE" "Файл с котировками"]])

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

(defn app
  [& args]
  (let [opts      (parse-opts args cli-options :in-order true)
        arguments (:arguments opts)
        cmd       (first arguments)]
    (cond
      (= cmd "cashflow") (cashflow (global-config opts) (rest arguments))
      (= cmd "budget")   (budget   (global-config opts) (rest arguments))
      :else              (println "Unknown command. Available commands: cashflow, budget\n"
                                  (:summary opts)))))

(defn -main
  [& args]
  (apply app args)
  ; System/exit нужен из-за conch, у которого что-то подвисает при вызове
  ; внешнего процесса ledger
  (System/exit 0))
