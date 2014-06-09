(ns ledger-report.dates
  (:require [clj-time.core   :as t]
            [instaparse.core :as insta]
            [clojure.core.match :refer [match]]
            [clojure.string :as s]))

(def parser
  (insta/parser
    "
    Expression = Period | Period <'..'> Period
    Period = Year | Month | Month <#'\\s+'> Year | DigitMonth <'.'> Year | Date
    Year = #'\\d{4}'
    Date = Year <'/'> DigitMonth <'/'> Day
    Month = #'jan(uary)?|feb(ruary)?|mar(ch)?|apr(il)?|may|june?|july?|aug(ust)?|sep(tember)?|oct(ober)?|nov(ember)?|dec(ember)?'
    DigitMonth = #'0?[1-9]|1[12]'
    Day = #'[12][0-9]|3[01]|0?[1-9]'
    "))

(defn current-year [] (t/year (t/today)))

(def month-names
  {
   "january" 1, "jan" 1, "february" 2, "feb" 2, "march" 3, "mar" 3, "april" 4, "apr" 4,
   "may" 5, "june" 6, "jun" 6, "july" 7, "jul" 7, "august" 8, "aug" 8, "september" 9, "sep" 9,
   "october" 10, "oct" 10, "november" 11, "nov" 11, "december" 12, "dec" 12
  })

(defn parse-month
  "Превращает строковое представление месяца в число от 1 до 12"
  [s]
  (if-let [index (month-names s)]
    index
    (if (re-matches #"0?[1-9]|1[12]" s)
      (Integer/parseInt s)
      (throw (IllegalArgumentException. (str "Invalid month: " s))))))

(defn make-month-year-period
  "Делает период в 1 месяц или в 1 год, в зависимости от year и month, которые
   порознь могут быть nil"
  [year month]
  (let [year (if year (Integer/parseInt year) (current-year))]
    (if month
      (let [beg (t/local-date year (parse-month month) 1)
            end (t/plus beg (t/months 1))]
        [beg end])

      (let [beg (t/local-date year 1 1)
            end (t/plus beg (t/years 1))]
        [beg end]))))

(defn match-one-period
  "Парсит один временной период"
  [period]
  (match period
         [[:Year y]]                  (make-month-year-period y nil)
         [[:Month m]]                 (make-month-year-period nil m)
         [[:DigitMonth m] [:Year y]]  (make-month-year-period y m)
         [[:Month m]      [:Year y]]  (make-month-year-period y m)))

(defn merge-periods
  "Слияние двух периодов"
  [[a _] [_ b]]
  [a b])

(defn parse-period
  "Парсит одиночный период или диапазон"
  [period]
  (match (parser (s/lower-case period))
         [:Expression [:Period & beg] [:Period & end]]
         (merge-periods (match-one-period beg)
                        (match-one-period end))
         ;;
         [:Expression [:Period & beg]]
         (match-one-period beg)
         ))

(defn iso-date
  [date]
  (.toString date))
