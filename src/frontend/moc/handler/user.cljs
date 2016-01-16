(ns moc.handler.user
  (:require [re-frame.core :refer [dispatch register-handler]]
            [moc.ajax :as ajax]
            [moc.router :as router]))

(register-handler
 :user/get-current
 (fn [db _]
   (ajax/request {:path [:api.user/me]
                  :on-success #(dispatch [:user/get-current-success %])})
   db))

(register-handler
 :user/get-current-success
 (fn [db [_ user]]
   (when (= :url/index (-> db :route/info :handler))
     (router/navigate! [:url.user/password]))
   (assoc db :user/current user)))
