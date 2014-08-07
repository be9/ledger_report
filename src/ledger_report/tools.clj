(ns ledger-report.tools
  (:require [clojure.java.io :as io]
            [clojure.edn     :as edn]))

(defn read-edn
  "Открывает и читает файл данных filename в формате EDN. Возвращает прочитанную
   структуру данных или nil."
  [filename]
  (try
    (with-open [infile (java.io.PushbackReader. (io/reader filename))]
      (edn/read infile))
    (catch java.io.FileNotFoundException e
      nil)))
