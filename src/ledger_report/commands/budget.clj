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
            ;[ledger-report.formatters.table :as table]
            ))

(def cli-options
  [["-d" "--debug"         "Отладочная информация"]])

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
                             (vals budget)))]
    ; reduce-kv
    (reduce-kv
      (fn [[result remainder] acc v]
        (if-let [k (register/find-longest-prefix result acc)]
          ; Ключ найден. Прибавляем значение
          [(update-in result [k :real] + (.getAmount v)) remainder]
          [result (assoc remainder acc v)]))
      [initial {}]
      item-map)))

;;;;;

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
            ]
        ;(println budget-data)
        ;(println (budget-period budget-data))
        ;(println (map-items (earnings-seq cashflow) (:mapping budget-data)))
        (println (apply-budget expense-items expense-budget))


        )

      ) ))
