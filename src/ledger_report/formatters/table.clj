(ns ledger-report.formatters.table
  (:require [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]
            [clojurewerkz.money.format  :as mf])

  (:import [org.joda.money.format MoneyFormatterBuilder]
           java.util.Locale))

(def currency-formatter
  (-> (MoneyFormatterBuilder.)
      .appendAmountLocalized
      (.appendLiteral " ")
      .appendCurrencySymbolLocalized
      .toFormatter))                  ; "22 111 230,00 руб."

(def locale (Locale. "ru" "RU"))

(defn format-value
  [value]
  (mf/format value currency-formatter locale))

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
                                 (map #(count (format-value (% 1)))
                                      data))

        format-string     (str "%-" max-account-width "s | %" max-amount-width "s | %6.2f%%")

        sep               (apply str (take (+ max-account-width max-amount-width 13) (repeat "-")))

        ;_                 (println max-account-width max-amount-width format-string)

        table (mapv (fn [[account value percentage]]
                      (format format-string
                              (account-name account)
                              (format-value value)
                              (* percentage 100.0))) data)]
      (concat [sep] (pop table) [sep (last table)])))
