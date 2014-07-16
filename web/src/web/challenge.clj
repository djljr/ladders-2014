(ns web.challenge
  (:require [clojure.java.jdbc :as sql]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/ladders?user=ladders&password=password"))

(defn all []
  (sql/query spec ["select * from common.challenge"]))

(defn all-full [current-user]
  (sql/query spec ["select o.username, 1 as rank from common.challenge c join common.user o on o.id = c.challenger_user where opponent_user = ?" current-user]))

(defn create! [challenger opponent]
  (let [post (first (sql/insert! spec :common.post {:creator_user 1}))
        post_id (:id post)]
    (sql/insert! spec :common.challenge {:challenger_user 1 :opponent_user 2 :post_id post_id :challenge_date (c/to-sql-time (t/now))})))