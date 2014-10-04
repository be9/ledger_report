(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [ledger-report.core :refer :all]
            [ledger-report.dates :as d]
            [clj-time.core :as t]
            [clojure.stacktrace :refer [e]]))

(defn my-ledger-file
  [basename]
  (str (System/getenv "HOME") "/Dropbox/shared/ledger/" basename))

(defn cf
  [& args]
  (apply app "-f" (my-ledger-file "my.ldg") "-m" (my-ledger-file "accounts.edn") "cashflow" "-p" args))

(defn bdg
  []
  (app "-f" (my-ledger-file "my.ldg") "-m" (my-ledger-file "accounts.edn") "budget" "-d" (my-ledger-file "budgets/2014_07.edn")))

(defn bdgaug
  []
  (app "-f" (my-ledger-file "my.ldg") "-m" (my-ledger-file "accounts.edn") "budget" "-d" (my-ledger-file "budgets/2014_08.edn")))

(defn bdgsep
  []
  (app "-f" (my-ledger-file "my.ldg") "-m" (my-ledger-file "accounts.edn") "budget" "-d" (my-ledger-file "budgets/2014_09.edn")))
