(defproject web "0.1.0-SNAPSHOT"
  :description "ladde.rs web"
  :url "http://ladde.rs"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/friend "0.2.1"]
                 [enlive "1.1.5"]
                 [compojure "1.1.8"]
                 [org.clojure/java.jdbc "0.3.4"]
                 [postgresql "9.3-1101.jdbc4"]
                 [clj-time "0.7.0"]]
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler web.core/app})

