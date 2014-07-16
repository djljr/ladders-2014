(ns web.user
  (:require [clojure.java.jdbc :as sql]
            [cemerick.friend.credentials :as creds]))


(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/ladders?user=ladders&password=password"))

(defn find-user [username]
  (first (sql/query spec ["select id, username, password from common.user where username = ?" username])))

(defn all []
  (sql/query spec ["select * from common.user"]))

(defn create! [{:keys [username password admin] :as user-data}]
  (let [bcrypt-password (creds/hash-bcrypt password)]
    (sql/insert! spec :common.user {:username username :password bcrypt-password})
    {:identity username :password bcrypt-password}))
