(ns ledger-report.formatters.table
  (:require [ledger-report.formatters.currency :as fc]))

(defn account-name
  [account]
  (cond
    (= account :total) "Итого:"
    (= account :other) "Прочее"
    (vector? account)  (clojure.string/join ":" account)
    :else              account))

(defn percentage-formatter
  "Форматирует таблицу [Счет, Денежная величина, Процент "
  [data]
  (let [max-account-width (apply max
                                 (map #(count (account-name (first %)))
                                      data))
        max-amount-width  (apply max
                                 (map #(count (fc/format-value (% 1)))
                                      data))

        format-string     (str "%-" max-account-width "s | %" max-amount-width "s | %6.2f%%")

        sep               (apply str (take (+ max-account-width max-amount-width 13) (repeat "-")))

        ;_                 (println max-account-width max-amount-width format-string)

        table (mapv (fn [[account value percentage]]
                      (format format-string
                              (account-name account)
                              (fc/format-value value)
                              (* percentage 100.0))) data)]
      (concat [sep] (pop table) [sep (last table)])))

(defn budget-table-formatter
  "Форматирует таблицу [Счет, Ден. величина 1, Ден. величина 2, Процент"
  [data]
  (let [max-account-width (apply max
                                 (map #(count (account-name (first %)))
                                      data))
        max-amount1-width  (apply max
                                 (map #(count (fc/format-value (% 1)))
                                      data))

        max-amount2-width  (apply max
                                 (map #(count (fc/format-value (% 2)))
                                      data))


        format-string     (str "%-" max-account-width "s | %" max-amount1-width "s | %" max-amount2-width "s | %6.2f%%")

        sep               (apply str (take (+ max-account-width max-amount1-width max-amount2-width 16) (repeat "-")))

        table (mapv (fn [[account value1 value2 percentage]]
                      (format format-string
                              (account-name account)
                              (fc/format-value value1)
                              (fc/format-value value2)
                              (* percentage 100.0))) data)]
      (concat [sep] (pop table) [sep (last table)])))
