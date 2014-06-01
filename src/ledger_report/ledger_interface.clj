(ns ledger-report.ledger-interface
  (:require [me.raynes.conch :refer [with-programs] :as sh]
            [clojure.string :as s]))

(def option-mapping {
  :file             "-f"
  :period           "--period"
  :related          "--related"
  :register-format  "--register-format"
  :display          "--display"
  :prices           "--price-db"
  })

(defn make-command-line
  [opts]
  (reduce (fn [cmdline [k v]]
            (if-let [mapped-opt (option-mapping k)]
              (cond
                (= v true) (conj cmdline mapped-opt)
                (= v nil)  cmdline
                :else      (conj cmdline mapped-opt v))
              (throw (IllegalArgumentException. (str "Invalid option " k)))))
          [] opts))

(defn register
  [expression opts]
  (with-programs [ledger]
    (let [ledger-options (make-command-line opts)
          cmd (conj ledger-options "register" expression)]

    (s/split (apply ledger cmd) #"\n"))))
