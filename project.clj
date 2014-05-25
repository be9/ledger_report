(defproject ledger_report "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [clojurewerkz/money "1.5.0"]
                 [me.raynes/conch "0.7.0"]]
  :main ^:skip-aot ledger-report.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
