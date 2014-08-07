(ns ledger-report.tools
  (:require [clojure.java.io            :as io]
            [clojure.edn                :as edn]
            [clojurewerkz.money.amounts :as ma]
            ))

(defn read-edn
  "Открывает и читает файл данных filename в формате EDN. Возвращает прочитанную
   структуру данных или nil."
  [filename]
  (try
    (with-open [infile (java.io.PushbackReader. (io/reader filename))]
      (edn/read infile))
    (catch java.io.FileNotFoundException e
      nil)))

;;

(defn make-currency-amount
  [s currency]
  (ma/amount-of currency
                (Float/parseFloat s)
                java.math.RoundingMode/HALF_UP))
