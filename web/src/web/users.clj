(ns web.users
  (:require [clojure.java.jdbc :as sql]
            [cemerick.friend.credentials :as creds]))


(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/ladders?user=ladders&password=password"))

(defn find-user [username]
  (sql/query spec ["select username, password from users where username = ?" username]))

(defn all []
  (sql/query spec ["select * from users"]))

(defn create [{:keys [username password admin] :as user-data}]
  (let [bcrypt-password (creds/hash-bcrypt password)]
    (println "create " username " " password)
    (sql/insert! spec :users {:username username :password bcrypt-password})
    {:identity username :password bcrypt-password}))