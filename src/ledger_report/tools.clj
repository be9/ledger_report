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
  [value currency]
  (ma/amount-of currency
                (if (string? value)
                  (Float/parseFloat value)
                  value)
                java.math.RoundingMode/HALF_UP))
