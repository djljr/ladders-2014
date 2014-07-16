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
            [clojure.string :as str]))

(html/deftemplate index "templates/index.html" 
  [] 
  [:div.users] (html/content (user/all))
  [:div.challenges] (html/content (challenge/all)))

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
    (friend/authenticated (index))))

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