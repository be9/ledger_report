(ns ledger-report.crunching.register
  (:require [clojure.string                 :as s]
            [ledger-report.ledger-interface :as ledger]
            [ledger-report.tools            :refer :all]
    ))

(defn- bare-cashflow
  "Возвращает результат, возвращённый ledger, как вектор строк.
   Каждая строка имеет формат:

   имя счёта\tсумма"
  [accounts file period prices]

  (ledger/register
    accounts
    {
     :file   file
     :period period
     :prices prices
     :related true
     :register-format "%(display_account)\t%(quantity(market(display_amount,d,\"р\")))\n"
     :display "not has_tag(\"SkipCashFlow\")"
     }))

(defn parsed-cashflow
  "Возвращает вектор разобранных результатов ledger, где каждой строке
   соответствует: [[Расходы То Се] 123.45]"
  [accounts file period prices currency]
  (map (fn [line]
         (let [[account value] (s/split line    #"\t")
                acc-parts      (s/split account #":")
                value          (make-currency-amount value currency)]
            [acc-parts value]))
       (bare-cashflow accounts file period prices)))
