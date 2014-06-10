(ns ledger-report.formatters.currency
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
