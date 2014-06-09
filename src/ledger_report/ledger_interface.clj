(ns ledger-report.ledger-interface
  (:require [me.raynes.conch :refer [with-programs] :as sh]
            [clojure.string :as s]
            [ledger-report.dates :refer [iso-date]]))

(def option-mapping {
  :file             "-f"
  :period           "--period"
  :related          "--related"
  :register-format  "--register-format"
  :display          "--display"
  :prices           "--price-db"
  })

(defn format-date-range
  [beg end]
  (str "from " (iso-date beg) " until " (iso-date end)))

(def option-value-formatting
  {
    :period (fn [p] (if (vector? p)
                      (apply format-date-range p)
                      p))
   })

(defn preformat-value
  [k v]
  (println k v)
  (if-let [formatter (option-value-formatting k)]
    (formatter v)
    v))

(defn make-command-line
  [opts]
  (reduce (fn [cmdline [k v]]
            (if-let [mapped-opt (option-mapping k)]
              (cond
                (= v true) (conj cmdline mapped-opt)
                (= v nil)  cmdline
                :else      (conj cmdline mapped-opt (preformat-value k v)))
              (throw (IllegalArgumentException. (str "Invalid option " k)))))
          [] opts))

(defn register
  [expression opts]
  (with-programs [ledger]
    (let [ledger-options (make-command-line opts)
          _ (println ledger-options)
          cmd (conj ledger-options "register" expression)]

    (s/split (apply ledger cmd) #"\n"))))
