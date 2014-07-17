(ns ledger-report.ledger-interface
  (:require [me.raynes.conch     :refer [with-programs] :as sh]
            [clojure.string      :as s]
            [ledger-report.dates :refer [iso-date]]))

(def option-mapping {
  :file             "-f"
  :period           "--period"
  :related          "--related"
  :register-format  "--register-format"
  :balance-format   "--balance-format"
  :display          "--display"
  :prices           "--price-db"
  :value            "-V"
})

(defn- format-date-range
  [beg end]
  (let [from-part  (when beg
                     (str "from " (iso-date beg)))
        until-part (when end
                     (str "until " (iso-date end)))]

    (s/trim (str from-part " " until-part))))

(def option-value-formatting
  {
    :period (fn [p] (if (vector? p)
                      (apply format-date-range p)
                      p))
  })

(defn- preformat-value
  [k v]
  (if-let [formatter (option-value-formatting k)]
    (formatter v)
    v))

(defn- make-command-line
  [opts]
  (reduce (fn [cmdline [k v]]
            (if-let [mapped-opt (option-mapping k)]
              (cond
                (= v true) (conj cmdline mapped-opt)
                (= v nil)  cmdline
                :else      (conj cmdline mapped-opt (preformat-value k v)))
              (throw (IllegalArgumentException. (str "Invalid option " k)))))
          [] opts))


(defn- ledger-command
  [command-name argument opts]
  (with-programs [ledger]
    (let [ledger-options (make-command-line opts)
          cmd            (flatten (conj ledger-options command-name argument))
          result         (s/split (apply ledger cmd) #"\n")]
      result

    )))

;;

(defn register
  [expression opts]
  (ledger-command "register" expression opts))

(defn balance
  [accounts opts]
  (ledger-command "balance" accounts opts))
