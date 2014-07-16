(ns web.core
  (:require [web.user :as user]
            [web.challenge :as challenge]
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
            [clojure.string :as str]))

(def ^:dynamic *challenge-sel* [[:.open-challenges (html/nth-of-type 1)] :> html/first-child])
(def ^:dynamic *player-list-sel* [[:.player-list (html/nth-of-type 1)] :> html/first-child])

(defn gravatar [name]
  (str "http://www.gravatar.com/avatar/" (hash/md5 name) "?d=identicon"))

(html/defsnippet challenge-card "templates/index.html" *challenge-sel* 
  [{:keys [username rank date]}]
  [:div.challenge-player-name] (html/content (str rank ". " username))
  [:div.challenge-player-icon :img] (html/set-attr :src (gravatar username))
  [:div.challenge-date] (html/content (fmt/unparse (fmt/formatter "yyyy-MM-dd HH:mm:ss") date)))

(html/defsnippet player-list "templates/index.html" *player-list-sel*
  [{:keys [id username rank]}]
  [:span.player-name] (html/content (str rank ". " username))
  [:span.player-icon] (html/set-attr :style (str "background-image:url(" (gravatar username) ")"))
  [:a.challenge-player] (html/set-attr :href (str "/challenge?opponent=" id)))

(html/deftemplate index "templates/index.html" 
  []
  [:.open-challenges] (html/content (map challenge-card (challenge/all-full (:id (friend/current-authentication)))))
  [:.player-list] (html/content (map player-list (user/all))))

(html/deftemplate login "templates/login.html" [])
(html/deftemplate error "templates/error.html" [])

(derive ::admin ::user)

(compojure/defroutes routes
  (GET "/" req (index))
  (GET "/login" req (login))
  (POST "/signup" {{:keys [username password confirm] :as params} :params :as req}
    (if (and (not-any? str/blank? [username password confirm])
             (= password confirm))
      (let [user (user/create! (select-keys params [:username :password :admin]))]
        (friend/merge-authentication
          (resp/redirect "/auth")
          user))
      (assoc (resp/redirect "/login") :flash "passwords don't match!")))
  (POST "/challenge" req
    (friend/authenticated 
        (let [user friend/current-authentication
              opponent (:opponent (req :params))]
            (let [ch (challenge/create! (:id user) opponent)]
              (index)))))
  (GET "/auth" req 
    (friend/authenticated (index)))
  (route/resources "/"))

(def app (handler/site
  (friend/authenticate
    routes
    {:allow-anon? true
     :login-uri "/login"
     :default-landing-uri "/"
     :unauthorized-handler #(-> (error)
                                resp/response
                                (resp/status 401))
     :credential-fn #(creds/bcrypt-credential-fn user/find-user %)
     :workflows [(workflows/interactive-form)]})))