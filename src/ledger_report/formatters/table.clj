(ns ledger-report.formatters.table
  (:require [ledger-report.formatters.currency :as fc]))

(defn account-name
  [account]
  (if (= account :total)
    "Итого:"
    account))

(defn formatter
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
