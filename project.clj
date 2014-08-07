(defproject ledger_report "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/core.match "0.2.1"]
                 [clojurewerkz/money "1.6.0"]
                 [me.raynes/conch "0.7.0"]
                 [clj-time "0.8.0"]
                 [instaparse "1.3.3"]]
  :main ledger-report.core
  :target-path "target/%s"
  :bin { :name "lrpt" }
  :profiles {:uberjar {:aot :all
                       :main ledger-report.core
                       :target-path "target/" }
             :dev {:source-paths ["dev"]
                   :main user
                   :dependencies [[org.clojure/tools.namespace "0.2.5"]
                                  [org.clojure/java.classpath "0.2.2"]]}})
