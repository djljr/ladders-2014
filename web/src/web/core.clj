(ns web.core
  (:require [web.user :as user]
            [web.ladder :as ladder]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [compojure.core :as compojure :refer (GET POST ANY defroutes)]
            (compojure [handler :as handler]
                       [route :as route])
            [ring.util.response :as resp]
            [net.cgrand.enlive-html :as html]
            [pandect.core :as hash]
            [clj-time.format :as fmt]
            [clj-time.coerce :as ct]
            [clojure.string :as str]))

(def ^:dynamic *challenge-sel* [[:.open-challenges (html/nth-of-type 1)] :> html/first-child])
(def ^:dynamic *accepted-sel* [[:.accepted-challenges (html/nth-of-type 1)] :> html/first-child])
(def ^:dynamic *player-list-sel* [[:.player-list (html/nth-of-type 1)] :> html/first-child])
(def ^:dynamic *ladder-sel* [[:.ladder-list (html/nth-of-type 1)] :> html/first-child])

(defn gravatar [name]
  (str "http://www.gravatar.com/avatar/" (hash/md5 name)))

(defn gravatar-ident [name]
  (str (gravatar name) "?d=identicon"))

(defn gravatar-retro [name]
  (str (gravatar name) "?d=retro"))

(defn format-date [date]
  (fmt/unparse (fmt/formatter "yyyy-MM-dd HH:mm:ss" (ct/from-sql-time date))))

(html/defsnippet challenge-card "templates/index.html" *challenge-sel* 
  [ladder {:keys [id username rank date]}]
  [:div.challenge-player-name] (html/content (str rank ". " username))
  [:div.challenge-player-icon :img] (html/set-attr :src (gravatar-ident username))
  [:div.challenge-date] (html/content (format-date date))
  [:.accept-challenge] (html/set-attr :href (str "/ladder/" ladder "/challenge/" id "/accept")))

(html/defsnippet player-list "templates/index.html" *player-list-sel*
  [ladder {:keys [id username rank]}]
  [:span.player-name] (html/content (str rank ". " username))
  [:span.player-icon] (html/set-attr :style (str "background-image:url(" (gravatar-ident username) ")"))
  [:a.challenge-player] (html/set-attr :href (str "/ladder/" ladder "/challenge/" id)))

(html/defsnippet game-card "templates/index.html" *accepted-sel*
  [ladder {:keys [id player1 player2 date]}]
  [:div.game-player1-icon :img] (html/set-attr :src (gravatar-ident player1))
  [:div.game-player2-icon :img] (html/set-attr :src (gravatar-ident player2))
  [:div.game-players] (html/content (player1 " vs. " player2))
  [:div.accepted-date] (html/content (format-date date))
  [:a.win-game] (html/set-attr :href (str "/ladder/" ladder "/challenge/" id "/win"))
  [:a.lose-game] (html/set-attr :href (str "/ladder/" ladder "/challenge/" id "/lose")))

(html/deftemplate index "templates/index.html" 
  [{:keys [id name owner]}]
  [:.open-challenges] (html/content 
    (map (partial challenge-card id) 
        (ladder/challenges-as-opponent id (:id (friend/current-authentication)))))
  [:.player-list] (html/content (map (partial player-list id) (user/all id)))
  [:.accepted-challenges] (html/content 
    (map (partial game-card id) 
         (ladder/games id (:id (friend/current-authentication)))))
  [:.ladder-admin] (html/content owner)
  [:.ladder-name] (html/content name))

(html/defsnippet ladder-list "templates/ladders.html" *ladder-sel*
  [{:keys [id name participants joinable?]}]
  [:div.ladder-icon :img] (html/set-attr :src (gravatar-retro name))
  [:span.ladder-name] (html/content (str name " (" participants ")"))
  [:a.join-ladder] (html/set-attr :href (str "/ladder/" id "/join"))
  [:a.ladder-link] (html/set-attr :href (str "/ladder/" id)))

(html/deftemplate ladders "templates/ladders.html" 
  [user]
  [:.ladder-list] (html/content (map ladder-list (ladder/all-meta))))

(html/deftemplate login "templates/login.html" [])
(html/deftemplate error "templates/error.html" [])

(derive ::admin ::user)

(compojure/defroutes routes
  (GET "/" req 
    (if-let [user friend/current-authentication]
        (resp/redirect "/ladders")
        (login)))
  (GET "/login" req (login))
  (POST "/signup" {{:keys [username password confirm] :as params} :params :as req}
    (if (and (not-any? str/blank? [username password confirm])
             (= password confirm))
      (let [user (user/create! (select-keys params [:username :password :admin]))]
        (friend/merge-authentication
          (resp/redirect "/ladders")
          user))
      (assoc (resp/redirect "/login") :flash "passwords don't match!")))
  (GET "/ladders" req
    (if-let [user friend/current-authentication]
        (ladders user)
        (resp/redirect "/login")))
  (POST "/ladders/create" {{:keys [name] :as params} :params :as req}
    (friend/authenticated
      (ladder/create! name (:id (friend/current-authentication))))
    (resp/redirect "/login"))
  (GET "/ladder/:id" [id]
    (friend/authenticated
      (index (ladder/find-ladder (Long/parseLong id)))))
  (GET "/ladder/:id/join" [id]
    (friend/authenticated
      (ladder/join! (Long/parseLong id) (:id (friend/current-authentication)))
      (resp/redirect (str "/ladder/" id))))
  (GET "/ladder/:id/challenge/:opponent" [id opponent]
    (friend/authenticated
      (ladder/challenge! (Long/parseLong id) (:id (friend/current-authentication)) (Long/parseLong opponent))
      (resp/redirect (str "/ladder/" id))))
  (GET "/ladder/:ladder/challenge/:challenge/accept" [ladder challenge]
    (friend/authenticated
      (ladder/challenge-accepted! (Long/parseLong challenge))
      (resp/redirect (str "/ladder" ladder))))
  (route/resources "/"))

(def app (handler/site
  (friend/authenticate
    routes
    {:allow-anon? true
     :login-uri "/login"
     :default-landing-uri "/ladders"
     :unauthorized-handler #(-> (error)
                                resp/response
                                (resp/status 401))
     :credential-fn #(creds/bcrypt-credential-fn user/find-user %)
     :workflows [(workflows/interactive-form)]})))