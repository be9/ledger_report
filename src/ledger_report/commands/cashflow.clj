(ns ledger-report.commands.cashflow
  (:require [clojure.tools.cli          :refer [parse-opts]]
            [clojure.java.io            :as io]
            [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]
            [ledger-report.ledger-interface :as ledger]
            [ledger-report.dates :as dates]
            [ledger-report.formatters.currency :as fc]
            [ledger-report.formatters.table :as table]))

(defn- register-results
  "Возвращает результат, возвращённый ledger, как вектор строк.
   Каждая строка имеет формат:

   имя счёта\tсумма"
  [opts]

  (ledger/register
    ((opts :metadata) :assets_for_cashflow)
    {
     :file   (:file opts)
     :period (:period opts)
     :prices (:prices opts)
     :related true
     :register-format "%(display_account)\t%(quantity(market(display_amount,d,\"р\")))\n"
     :display "not has_tag(\"SkipCashFlow\")"
     }))
;;

(defn- make-currency-amount
  [s currency]
  (ma/amount-of currency
                (Float/parseFloat s)
                java.math.RoundingMode/HALF_UP))

(defn- parse-register-results
  "Возвращает вектор разобранных результатов ledger, где каждой строке
   соответствует: [[Расходы То Се] 123.45]"
  [bare-results currency]
  (map (fn [line]
         (let [[account value] (s/split line    #"\t")
                acc-parts      (s/split account #":")
                value          (make-currency-amount value currency)]
            [acc-parts value]))
       bare-results))

;;

(defn- group-name-for-account
  "Находит для account соответствие в account-map по префиксу.
   Если ничего не найдено, возвращает default-name"
  [account account-map default-name]

  (loop [prefix account]
    (if (empty? prefix)
      default-name
      (if-let [group (account-map prefix)]
        group
        (recur (pop prefix))))))    ; Пытаемся найти максимальный префикс

(defn- collapse-accounts
  "Группирует данные по отдельным счетам в один большой map. grouper возвращает имя
   группы для каждого счёта"
  [data grouper]

  (reduce
    (fn [accounts [acc value]]
      (let [group (grouper acc)]
        (if-let [accum (accounts group)]
          (assoc accounts group (ma/plus accum value))
          (assoc accounts group value))))
    {}
    data))

(defn- collapse-by-group
  [data account-map default-name]

  (let [grouper (fn [x] (group-name-for-account x account-map default-name))]
    (collapse-accounts data grouper)))

(defn- collapse-by-name
  [data account-map default-name]

  (let [grouper (fn [x] (s/join ":" x))]
    (collapse-accounts data grouper)))

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
   ["-n" "--no-group"      "Не группировать счета"]])

(defn cashflow
  [config args]
  (let [options   (parse-opts args cli-options)
        options   (merge config (:options options))
        options   (conj options
                        [:period (dates/parse-period (:period options))])
        results   (parse-register-results (register-results options)
                                          (:currency config))
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
