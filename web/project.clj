(defproject web "0.1.0-SNAPSHOT"
  :description "ladde.rs web"
  :url "http://ladde.rs"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/friend "0.2.1"]
                 [enlive "1.1.5"]
                 [compojure "1.1.8"]]
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler web.core/app})

