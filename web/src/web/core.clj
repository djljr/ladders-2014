(ns web.core
  (:require [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [compojure.core :as compojure :refer (GET POST ANY defroutes)]
            (compojure [handler :as handler]
                       [route :as route])
            [ring.util.response :as resp]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as str]))

(def users (atom {
    "user" {:username "user"
            :password (creds/hash-bcrypt "user")
            :roles #{::user}}}))

(html/deftemplate index "templates/index.html" 
  [] 
  [:span.users] (html/content @users))

(html/deftemplate login "templates/login.html" [])
(html/deftemplate error "templates/error.html" [])

(derive ::admin ::user)

(defn- create-user 
  [{:keys [username password admin] :as user-data}]
  (println (str "createuser " username " " password " " admin " " @users))
  (swap! users assoc username (-> (dissoc user-data :admin)
      (assoc :identity username
             :password (creds/hash-bcrypt password)
             :roles (into #{::user} (when admin [::admin]))))))

(compojure/defroutes routes
  (GET "/" req (index))
  (GET "/login" req (login))
  (POST "/signup" {{:keys [username password confirm] :as params} :params :as req}
    (if (and (not-any? str/blank? [username password confirm])
             (= password confirm))
      (let [user (create-user (select-keys params [:username :password :admin]))]
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
     :credentials-fn #(creds/bcrypt-credential-fn @users %)
     :workflows [(workflows/interactive-form)]})))