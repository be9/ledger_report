(ns ledger-report.commands.budget
  #_(:require [clojure.tools.cli          :refer [parse-opts]]
            [clojure.java.io            :as io]
            [clojure.string             :as s]
            [clojurewerkz.money.amounts :as ma]
            [ledger-report.ledger-interface :as ledger]
            [ledger-report.dates :as dates]
            [ledger-report.formatters.currency :as fc]
            [ledger-report.formatters.table :as table]))

(defn budget
  [config args]
  (println config args))
