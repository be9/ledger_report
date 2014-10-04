(ns ledger-report.commands.budget
  (:require [clojure.tools.cli                :refer [parse-opts]]
            [ledger-report.tools              :refer :all]
            [ledger-report.dates              :as dates]
            [ledger-report.crunching.register :as register]
            ;[clojure.java.io            :as io]
            [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]
            ;[ledger-report.ledger-interface :as ledger]
            ;[ledger-report.formatters.currency :as fc]
            [ledger-report.formatters.table :as table]
            ))

(defn read-budget-data
  "Читает данные о бюджете из файла"
  [filename]
  (read-edn filename))

(defn budget-period
  "Возвращает отпарсенный период из бюджета"
  [budget-data]
  (dates/parse-period (:period budget-data)))

(defn incomes-seq
  "Выбирает из данных о приходах и расходах приходы. "
  [data]
  (map
    (fn [[acc v]] [acc (ma/abs v)])
    (filter #(ma/negative? (% 1)) data)))

(defn expenses-seq
  "Выбирает из данных о приходах и расходах расходы"
  [data]
  (filter #(ma/positive? (% 1)) data))

(defn map-items
  "Превращает данные о доходах или расходах в map, трансформируя имена с учётом
  mapping. Если отображение не находится, в map попадает имя счёта в форме
  vector"
  [items mapping]

  (register/group-account-data
    (fn [acc]
      (register/map-account-name acc mapping acc))
    items))


(defn apply-budget
  "Применяет бюджет budget к map с реально понесенными доходами или расходами.
  Возвращает вектор [applied remainder], где applied - map с ключами из budget
  и значениями формата { :planned 123, :real 333 }, а remainder - остаток из
  item-map, для которого не были найдены строки в budget"
  [item-map budget]

  (let [; Сформируем структуру данных в итоговом виде
        initial (zipmap (keys budget)
                        (map (fn [v] { :planned v, :real 0 })
                             (vals budget)))

        initial (merge {:other {:planned 0, :real 0}} initial)]
    (reduce-kv
      (fn [[result remainder] acc v]
        (if-let [k (register/find-longest-prefix result acc)]
          ; Ключ найден. Прибавляем значение
          [(update-in result [k :real]      + (.getAmount v)) remainder]
          ; Ключ не найден. Кладём в remainder
          [(update-in result [:other :real] + (.getAmount v)) (assoc remainder acc v)]))
      [initial {}]
      item-map)))

(defn- ratio
  [x y]
  (if (zero? y) 0 (/ (double x) y)))

(defn process-budgeting-results
  "Сортирует results по убыванию ключа planned, вычисляет итог и процентное соотношение.
  Возвращает вектор векторов [name value-planned value-real percentage].
  Последним идет name :total"
  [results currency]

  (let [planned-total (reduce + (map :planned (vals results)))
        real-total    (reduce + (map :real    (vals results)))

        ; сортировка по убывающей сумме
        sorted       (sort-by #(- (:planned (% 1))) results)]

    (conj
      (mapv (fn [[group-name {real :real, planned :planned}]]
              [group-name
               (make-currency-amount planned currency)
               (make-currency-amount real currency)
               (ratio real planned)])
            sorted)
      [:total
       (make-currency-amount planned-total currency)
       (make-currency-amount real-total currency)
       (ratio real-total planned-total)])))

;;;;;

(def cli-options
  [["-d" "--debug"     "Отладочная информация"]])
  [["-r" "--remainder" "Детально показать то, что не попало в бюджет"]]

(defn budget
  [config args]
  (let [parsed-opts (parse-opts args cli-options)
        options     (merge config (:options parsed-opts))
        args        (:arguments parsed-opts)
        ;_           (println options args)
        ]
    (if (not= (count args) 1)
      (println "Требуется указать имя файла с данными бюджета")

      (let [budget-data     (read-budget-data (first args))
            budget-cashflow ((budget-data :budgets) :cashflow)
            cashflow        (register/parsed-cashflow
                              (budget-cashflow :accounts)
                              (:file options)
                              (budget-period budget-data)
                              (:prices options)
                              (:currency options))

            expense-items (map-items (expenses-seq cashflow) (:mapping budget-data))
            expense-budget (budget-cashflow :expenses)
            [expense-results expense-rem] (apply-budget expense-items expense-budget)


            income-items (map-items (incomes-seq cashflow) (:mapping budget-data))
            income-budget (budget-cashflow :incomes)
            [income-results income-rem] (apply-budget income-items income-budget)


            ]
        ;(println budget-data)
        ;(println (budget-period budget-data))
        ;(println (map-items (earnings-seq cashflow) (:mapping budget-data)))
        (println
          (s/join "\n" (table/budget-table-formatter
                         (process-budgeting-results expense-results (:currency options)))))

        (println expense-rem)

        (println
          (s/join "\n" (table/budget-table-formatter
                         (process-budgeting-results income-results (:currency options)))))

        (println income-rem)
        )

      ) ))
