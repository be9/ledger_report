(ns ledger-report.dates-test
  (:require [clojure.test :refer :all]
            [ledger-report.dates :refer :all]
            [clj-time.core :as t]))

(deftest parse-period-test
  (testing "Year is parsed"
    (is (= (parse-period "2014")
           [(t/local-date 2014 1 1) (t/local-date 2015 1 1)])))

  (testing "Month is parsed"
    (is (= (parse-period "Jan")
           [(t/local-date (t/year (t/today)) 1 1) (t/local-date (t/year (t/today)) 2 1)])))

  (testing "Year and month is parsed"
    (is (= (parse-period "Dec 2014")
           [(t/local-date 2014 12 1) (t/local-date 2015 1 1)]))

    (is (= (parse-period "Jun 2009")
           [(t/local-date 2009 6 1) (t/local-date 2009 7 1)]))

    (is (= (parse-period "06.2009")
           [(t/local-date 2009 6 1) (t/local-date 2009 7 1)])))

  #_(testing "Date parsing"

    (is (= (parse-period "2014/06/17")
           [(t/local-date 2014 6 17) (t/local-date 2014 6 18)]))
    
    )


  (testing "Ranges"

    (is (= (parse-period "Dec 2014..Mar 2015")
           [(t/local-date 2014 12 1) (t/local-date 2015 4 1)]))

    )

  )
