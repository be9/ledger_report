(ns ledger-report.crunching.register
  (:require [clojure.string                 :as s]
            [ledger-report.ledger-interface :as ledger]
            [ledger-report.tools            :refer :all]
            [clojurewerkz.money.amounts     :as ma]
    ))

(defn- bare-cashflow
  "Возвращает результат, возвращённый ledger, как вектор строк.
   Каждая строка имеет формат:

   имя счёта\tсумма"
  [accounts file period prices]

  (ledger/register
    accounts
    {
     :file   file
     :period period
     :prices prices
     :related true
     :register-format "%(display_account)\t%(quantity(market(display_amount,d,\"р\")))\n"
     :display "not has_tag(\"SkipCashFlow\")"
     }))

;;

(defn parsed-cashflow
  "Возвращает вектор разобранных результатов ledger, где каждой строке
   соответствует: [[Расходы То Се] 123.45]"
  [accounts file period prices currency]
  (map (fn [line]
         (let [[account value] (s/split line    #"\t")
                acc-parts      (s/split account #":")
                value          (make-currency-amount value currency)]
            [acc-parts value]))
       (bare-cashflow accounts file period prices)))

;;

(defn group-account-data
  "Группирует данные по отдельным счетам в один большой map. Функция grouper
  возвращает имя группы для каждого счёта"
  [grouper data]

  (reduce
    (fn [accounts [acc value]]
      (let [group (grouper acc)]
        (if-let [accum (accounts group)]
          (assoc accounts group (ma/plus accum value))
          (assoc accounts group value))))
    {}
    data))

;;

(defn find-longest-prefix
  "Ищет в m ключ [k1 k2 k3 ... kn] максимальной длины, постепенно обрезая ключ
  с конца. Если ничего не найдено, возвращает nil, в противном случае
  возвращает найденный ключ [k1 k2 ... km], где m <= n."
  [m k]

  (loop [prefix k]
    (if (empty? prefix)
      nil
      (if (contains? m prefix)
        prefix
        (recur (pop prefix))))))     ; Пытаемся найти максимальный префикс 

(defn map-account-name
  "Находит для account соответствие в account-map по префиксу.
   Если ничего не найдено, возвращает default-name"
  [account account-map default-name]

  (if-let [prefix (find-longest-prefix account-map account)]
    (get account-map prefix)
    default-name))
