(ns web.core
  (:require [web.users :as users]
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
  [:span.users] (html/content (users/all)))

(html/deftemplate login "templates/login.html" [])
(html/deftemplate error "templates/error.html" [])

(derive ::admin ::user)

(compojure/defroutes routes
  (GET "/" req (index))
  (GET "/login" req (login))
  (POST "/signup" {{:keys [username password confirm] :as params} :params :as req}
    (if (and (not-any? str/blank? [username password confirm])
             (= password confirm))
      (let [user (users/create (select-keys params [:username :password :admin]))]
        (friend/merge-authentication
          (resp/redirect "/auth")
          user))
      (assoc (resp/redirect "/login") :flash "passwords don't match!")))
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
     :credentials-fn #(creds/bcrypt-credential-fn users/find-user %)
     :workflows [(workflows/interactive-form)]})))