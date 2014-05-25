(ns ledger-report.commands.cashflow
  (:require [clojure.tools.cli          :refer [parse-opts]]
            [clojure.java.io            :as io]
            [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]))

(defn ledger-results
  "Возвращает результат, возвращённый ledger, как вектор строк.
   Каждая строка имеет формат:

   имя счёта\tсумма"
  [filename]
  (with-open [rdr (io/reader filename)]
    (into [] (line-seq rdr))))

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
  (println "earnings")
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
      [:total (ma/abs total) 100])))
;;

(def cli-options
  [["-p" "--period PERIOD" "За какой период показывать"]])

(defn cashflow
  [config args]
  (let [opts     (parse-opts args cli-options)
        results  (parse-ledger-results (ledger-results (:file config))
                                       (:currency config))
        metadata (config :metadata)
        earnings (process-account-data (earnings-data results metadata))
        payments (process-account-data (payments-data results metadata))]

    (println earnings)  
    (println payments)))
