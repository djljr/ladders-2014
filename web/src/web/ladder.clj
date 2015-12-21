(ns web.ladder
  (:require [clojure.java.jdbc :as sql]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/ladders?user=ladders&password=ladders"))

(defn find-ladder [ladder]
  (first (sql/query spec [
    "select l.id, l.name, o.username as owner
      from common.ladder l
      join common.user o on l.owner_user = o.id
     where l.id = ?" ladder])))

(defn all-meta []
  (sql/query spec [
    "select id, name, count(lu.user_id) as participants, true as \"joinable?\" 
      from common.ladder l
      left join common.ladder_user lu on l.id = lu.ladder_id
     group by l.id, l.name"]))

(defn challenges-as-opponent [ladder current-user]
  (sql/query spec [
    "select c.id, o.username, 1 as rank, challenge_date as date 
      from common.challenge c 
      join common.user o on o.id = c.challenger_user
     where c.opponent_user = ? and c.ladder_id = ? and accepted_date is null" current-user ladder]))

(defn challenges-as-challenger [ladder current-user]
  (sql/query spec [
    "select c.id, o.username, 1 as rank, challenge_date as date 
      from common.challenge c 
      join common.user o on o.id = c.opponent_user
     where c.challenger_user = ? and c.ladder_id = ? and accepted_date is null" current-user ladder]))

(defn games [ladder current-user]
  (sql/query spec [
    "select c.id, o.username as player1, ch.username as player2, c.accepted_date as date
      from common.challenge c
      join common.user o on o.id = c.opponent_user
      join common.user ch on ch.id = c.challenger_user
     where c.ladder_id = ? and 
           c.accepted_date is not null and 
           (c.challenger_user = ? or c.opponent_user = ?)" ladder current-user current-user]))

(defn time-now []
  (c/to-sql-time (t/now)))

(defn create! [name owner]
  (sql/insert! spec :common.ladder {:name name :owner_user owner :created_date (time-now)}))

(defn join! [ladder user]
  (sql/insert! spec :common.ladder_user {:ladder_id ladder :user_id user}))

(defn challenge! [ladder challenger opponent]
  (let [post (first (sql/insert! spec :common.post {:creator_user challenger}))
        post_id (:id post)]
    (sql/insert! spec :common.challenge {:ladder_id ladder :challenger_user challenger :opponent_user opponent :post_id post_id :challenge_date (time-now)})))

(defn challenge-accepted! [challenge]
  (sql/update! spec :common.challenge {:accepted_date (time-now)} ["id = ?" challenge]))