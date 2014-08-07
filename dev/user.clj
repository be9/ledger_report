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
  [period]
  (app "-f" (my-ledger-file "my.ldg") "-m" (my-ledger-file "accounts.edn") "cashflow" "-p" period))
