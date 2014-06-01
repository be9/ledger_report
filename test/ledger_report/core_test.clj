(ns ledger-report.core-test
  (:require [clojure.test :refer :all]
            [ledger-report.core :refer :all]))

#_(deftest coretest
  (testing "Year is parsed"
    (is (= (parse-period "2014") []))))
