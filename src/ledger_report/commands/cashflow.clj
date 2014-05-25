(ns ledger-report.commands.cashflow
  (:require [clojure.tools.cli          :refer [parse-opts]]
            [clojure.java.io            :as io]
            [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]
            [me.raynes.conch            :refer [with-programs] :as sh]
            [ledger-report.formatters.table :as table]))

(defn ledger-results
  "Возвращает результат, возвращённый ledger, как вектор строк.
   Каждая строка имеет формат:

   имя счёта\tсумма"
  [opts]

  (with-programs [ledger]
    (let [ledger-options ["-f"       (:file opts)
                          "--period" (:period opts)
                          "--related"
                          "--register-format"
                          "%(display_account)\t%(quantity(market(display_amount,d,\"р\")))\n"
                          "--display"
                          "not has_tag(\"SkipCashFlow\")"]
          ledger-options  (if-let [pricedb (:prices opts)]
                            (conj ledger-options "--price-db" pricedb)
                            ledger-options)

          cmd (conj ledger-options "register" "Активы")]

    (s/split (apply ledger cmd) #"\n"))))

;;

(defn parse-ledger-results
  "Возвращает вектор разобранных результатов ledger, где каждой строке
   соответствует: [[Расходы То Се] 123.45]"
  [bare-results currency]
  (map (fn [line]
         (let [[account value] (s/split line    #"\t")
                acc-parts      (s/split account #":")
                value          (ma/amount-of currency
                                             (Float/parseFloat value)
                                             java.math.RoundingMode/HALF_UP)]
            [acc-parts value]))
       bare-results))

;;

(defn group-name-for-account
  "Находит для account соответствие в account-map по префиксу.
   Если ничего не найдено, возвращает default-name"
  [account account-map default-name]

  (loop [prefix account]
    (if (empty? prefix)
      default-name
      (if-let [group (account-map prefix)]
        group
        (recur (pop prefix))))))    ; Пытаемся найти максимальный префикс

(defn collapse-accounts
  "Объединяет данные по отдельным счетам в один большой map"
  [data account-map default-name]

  (reduce
    (fn [accounts [acc value]]
      (let [group (group-name-for-account acc account-map default-name)]

        (if-let [accum (accounts group)]
          (assoc accounts group (ma/plus accum value))
          (assoc accounts group value))))
    {}
    data))

(defn earnings-data
  "Отбирает из данных поступления, группирует их по статьям и возвращает map
   Статья -> Сумма"
  [data metadata]
  (collapse-accounts
    (filter #(ma/negative? (% 1)) data)  ; отбираем только отрицательные величины
    (:earnings metadata)
    "Прочие доходы"))

(defn payments-data
  "Отбирает из данных траты, группирует их по статьям и возвращает map
   Статья -> Сумма"
  [data metadata]
  (collapse-accounts
    (filter #(ma/positive? (% 1)) data)  ; отбираем только положительные величины
    (:payments metadata)
    "Прочие выплаты"))

;;

(defn process-account-data
  "Сортирует account-data по убыванию, вычисляет итог и процентное соотношение.
   Возвращает вектор векторов [name value percentage]. Последним идет name :total"
  [account-data]

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
      [:total (ma/abs total) 1])))
;;

(def cli-options
  [["-p" "--period=PERIOD" "За какой период показывать" :required true]
   ["-n" "--no-group"      "Не группировать счета"]])

(defn cashflow
  [config args]
  (let [opts     (parse-opts args cli-options)
        merged-options (merge config (:options opts))
        results  (parse-ledger-results (ledger-results merged-options)
                                       (:currency config))
        metadata (config :metadata)
        earnings (process-account-data (earnings-data results metadata))
        payments (process-account-data (payments-data results metadata))
        delta    (ma/minus ((last earnings) 1)
                           ((last payments) 1))]

    (println "Доходы:")
    (println (s/join "\n" (table/formatter earnings)))
    (println "\n")

    (println "Расходы:")
    (println (s/join "\n" (table/formatter payments)))

    (println)
    (println "Доходы — Расходы =" (table/format-value delta))))
