(ns ledger-report.commands.cashflow
  (:require [clojure.tools.cli          :refer [parse-opts]]
            [clojure.java.io            :as io]
            [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]
            [ledger-report.ledger-interface :as ledger]
            [ledger-report.tools            :refer :all]
            [ledger-report.dates :as dates]
            [ledger-report.formatters.currency :as fc]
            [ledger-report.crunching.register :as register]
            [ledger-report.formatters.table :as table]))

(defn- register-results
  [options]

  (register/parsed-cashflow
    ((options :metadata) :assets_for_cashflow)
    (:file options)
    (:period options)
    (:prices options)
    (:currency options)))
;;

(defn- collapse-by-group
  [data account-map default-name]

  (let [grouper (fn [x] (register/map-account-name x account-map default-name))]
    (register/group-account-data grouper data)))

(defn- collapse-by-name
  [data account-map default-name]

  (let [grouper (fn [x] (s/join ":" x))]
    (register/group-account-data grouper data)))

(defn- earnings-data
  "Отбирает из данных поступления, группирует их по статьям и возвращает map
   Статья -> Сумма"
  [collapser data metadata]
  (collapser
    (filter #(ma/negative? (% 1)) data)  ; отбираем только отрицательные величины
    (:earnings metadata)
    "Прочие доходы"))

(defn- payments-data
  "Отбирает из данных траты, группирует их по статьям и возвращает map
   Статья -> Сумма"
  [collapser data metadata]
  (collapser
    (filter #(ma/positive? (% 1)) data)  ; отбираем только положительные величины
    (:payments metadata)
    "Прочие выплаты"))

;;

(defn- process-account-data
  "Сортирует account-data по убыванию, вычисляет итог и процентное соотношение.
   Возвращает вектор векторов [name value percentage]. Последним идет name :total"
  [account-data zero]

  (if (empty? account-data)
    [[:total zero 1]]
    (let [total        (ma/total (vals account-data))
          amount-total (double (.getAmount (ma/abs total)))
          ; сортировка по убывающей сумме
          sorted       (sort-by #(ma/negated (ma/abs (% 1))) account-data)]

      (conj
        (mapv (fn [[group-name value]]
                (let [value (ma/abs value)]
                  [group-name value (/ (double (.getAmount value))
                                       amount-total)]))
              sorted)
        [:total (ma/abs total) 1]))))
;;

(defn- get-balance-for
  "Рассчитывает баланс на заданную дату"
  [date opts]
  (->
    ((opts :metadata) :assets_for_cashflow)
    (ledger/balance {
      :file   (:file opts)
      :period [nil date]
      :prices (:prices opts)
      :value  true
      :balance-format "%(partial_account(true))\t%(quantity(market(scrub(display_total),d,\"р\")))\n" })
    (last)
    (make-currency-amount (:currency opts))))

;;

(def cli-options
  [["-p" "--period=PERIOD" "За какой период показывать" :required true]
   ["-n" "--no-group"      "Не группировать счета"]
   ["-d" "--debug"         "Отладочная информация"]])

(defn cashflow
  [config args]
  (let [options   (parse-opts args cli-options)
        options   (merge config (:options options))
        options   (conj options
                        [:period (dates/parse-period (:period options))])
        results   (register-results options)
        metadata  (config :metadata)
        collapser (if (:no-group options) collapse-by-name collapse-by-group)
        zero      (ma/amount-of (:currency config) 0)
        earnings  (process-account-data (earnings-data collapser results metadata) zero)
        payments  (process-account-data (payments-data collapser results metadata) zero)
        incoming  (get-balance-for (first (:period options)) options)
        outgoing  (get-balance-for (last (:period options)) options)
        delta     (ma/minus ((last earnings) 1)
                            ((last payments) 1))
        asset-delta (ma/minus outgoing incoming)]

    (println (str "Входящий остаток: " (fc/format-value incoming) "\n"))

    (println "Приход:")
    (println (s/join "\n" (table/formatter earnings)))
    (println "\n")

    (println "Расход:")
    (println (s/join "\n" (table/formatter payments)))

    (println "\nИсходящий остаток:" (fc/format-value outgoing))
    (println "\nПриход    — Расходы  =" (fc/format-value delta))
    (println   "Исходящий — Входящий =" (fc/format-value asset-delta))))
