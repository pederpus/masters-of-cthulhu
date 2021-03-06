(ns moc.routes.user
  (:require [clojure.string :as str]
            [clj-uuid :as uuid]
            [clj-time.core :as ct]
            [bidi.bidi :as bidi]
            [ring.util.response :as ru]
            [crypto.password.bcrypt :as password]
            [moc.routes.dispatch :refer [routes]]
            [moc.validate.util :refer [validate]]
            [moc.validate.user :as validate.user]
            [moc.db.user :as db.user]
            [moc.db.token :as db.token]
            [moc.mail :refer [send-mail!]]
            [moc.util :as util]
            [moc.urls :refer [urls]])
  (:import java.util.UUID))

(defn login-url [req token]
  (util/join-urls (util/base-url (get-in req [:headers "referer"]))
                  (bidi/path-for urls :url.auth/token :token token)))

(defn login-msg [link valid-to]
  (str "A login url has been generated for you at " (util/base-url link)
       "\n\n"
       "Please follow this link to login: " link
       "\n\n"
       "The link will be active until clicked, or until: " (util/date-time-format valid-to)
       "\n\n"
       "---\n"
       "This email has been generated. Do not respond to this email."))

(defn- send-login-url! [req email token valid-to]
  (send-mail! {:to email
               :subject "Login instructions"
               :body (login-msg (login-url req token) valid-to)}))

(defn ensure-user-login [db email-map]
  (let [email-query (assoc email-map :fields ["id"])
        user (or (db.user/get-by-email db email-query)
                 (db.user/create<! db email-query))
        token (uuid/v4)
        valid-to (ct/plus (ct/now)
                          (ct/days 1))]
    (db.token/create! db {:token token
                          :user-id (:id user)
                          :valid-to valid-to})
    [token valid-to]))

(defmethod routes :api.auth/register [{:keys [component/db params] :as req}]
  (if-let [errors (validate params validate.user/register-schema)]
    {:status 400
     :body errors}
    (let [normalized-email-params (update params :email #(-> %
                                                             (str/trim)
                                                             (str/lower-case)))
          [token valid-to] (ensure-user-login db normalized-email-params)]
      (send-login-url! req (:email normalized-email-params) token valid-to)
      {:status 200})))

(defn- login-user [db user-id res]
  (let [login-token (uuid/v4)]
    (db.user/update-logged-in! db {:id user-id})
    (db.token/create! db {:token login-token
                          :user-id user-id
                          :valid-to (ct/plus (ct/now)
                                             (ct/days 3))})
    (ru/set-cookie res "moc" (str login-token)
                   {:path "/"
                    :max-age (* 60 60 24 3)})))

(defmethod routes :url.auth/token [{:keys [component/db params]}]
  (let [token-param (update params :token #(UUID/fromString %))
        token (db.token/get-valid-by-id db token-param)]
    (if token
      (->> (ru/redirect "/")
           (login-user db (:user-id token)))
      {:status 404})))

(defmethod routes :api.auth/login [{:keys [component/db params]}]
  (if-let [errors (validate params validate.user/login-schema)]
    {:status 400
     :body errors}
    (if-let [user (db.user/get-by-email db {:fields ["id" "password"]
                                            :email (:email params)})]
      (if (password/check (:password params) (:password user))
        (->> {:status 204}
             (login-user db (:id user)))
        {:status 400
         :body {:password "Wrong email or password"}})
      {:status 400
       :body {:password "Wrong email or password"}})))

(defmethod routes :api.auth/logout [{:keys [component/db] :as req}]
  (if-let [user (util/current-user req)]
    (do
      (db.token/clear-for-user! db user)
      (-> {:status 401}
          (ru/set-cookie "moc" "deleted" {:path "/"})))
    {:status 401}))

(defmethod routes :api.user/me [req]
  (if-let [user (util/current-user req)]
    {:status 200
     :body user}
    {:status 401}))

(defmethod routes :api.user/profile [{:keys [component/db params] :as req}]
  (if-let [uid (util/current-user-id req)]
    (let [{:keys [name email password]} params
          schema (if password
                   validate.user/profile-schema
                   validate.user/passwordless-profile-schema)
          db-map (if password
                   {:id uid
                    :fields ["name" "email" "password"]
                    :values [name email (password/encrypt password)]}
                   {:id uid
                    :fields ["name" "email"]
                    :values [name email]})]
      (if-let [errors (validate params schema)]
        {:status 400
         :body errors}
        (try
          (db.user/update! db db-map)
          {:status 200
           :body (util/current-user req)}
          (catch Exception e
            {:status 400
             :body {:email "Another user is registered with this email"}}))))
    {:status 401}))
