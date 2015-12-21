(ns web.user
  (:require [clojure.java.jdbc :as sql]
            [cemerick.friend.credentials :as creds]))


(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/ladders?user=ladders&password=ladders"))

(defn find-user [username]
  (first (sql/query spec ["select id, username, password from common.user where username = ?" username])))

(defn all [ladder]
  (sql/query spec [
    "select id, username, 1 as rank 
      from common.user u 
      join common.ladder_user lu on lu.user_id = u.id 
     where lu.ladder_id = ?" ladder]))

(defn create! [{:keys [username password admin] :as user-data}]
  (let [bcrypt-password (creds/hash-bcrypt password)]
    (sql/insert! spec :common.user {:username username :password bcrypt-password})
    (find-user username)))
