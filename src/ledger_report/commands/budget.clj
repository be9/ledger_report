(ns ledger-report.commands.budget
  (:require [clojure.tools.cli                :refer [parse-opts]]
            [ledger-report.tools              :refer :all]
            [ledger-report.dates              :as dates]
            [ledger-report.crunching.register :as register]
            ;[clojure.java.io            :as io]
            ;[clojure.string             :as s]
            ;[clojurewerkz.money.amounts :as ma]
            ;[ledger-report.ledger-interface :as ledger]
            ;[ledger-report.formatters.currency :as fc]
            ;[ledger-report.formatters.table :as table]
            ))

(def cli-options
  [["-d" "--debug"         "Отладочная информация"]])

(defn read-budget-data
  "Читает данные о бюджете из файла"
  [filename]
  (read-edn filename))

(defn budget-period
  "Возвращает отпарсенный период из бюджета"
  [budget-data]
  (dates/parse-period (:period budget-data)))

(defn budget
  [config args]
  (let [parsed-opts (parse-opts args cli-options)
        options     (merge config (:options parsed-opts))
        args        (:arguments parsed-opts)
        ;_           (println options args)
        ]
    (if (not= (count args) 1)
      (println "Требуется указать имя файла с данными бюджета")

      (let [budget-data     (read-budget-data (first args))
            budget-cashflow ((budget-data :budgets) :cashflow)
            cashflow        (register/parsed-cashflow
                              (budget-cashflow :accounts)
                              (:file options)
                              (budget-period budget-data)
                              (:prices options)
                              (:currency options)) 
            
            ]
        ;(println budget-data)
        ;(println (budget-period budget-data))
        (println cashflow)


        )

      ) ))
