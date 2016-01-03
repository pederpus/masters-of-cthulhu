(ns moc.ui.authentication.login
  (:require [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [moc.ui.common.box :refer [box]]
            [moc.ui.common.icon-input :refer [icon-input]]
            [moc.ui.common.button :refer [button]]
            [moc.ui.common.link :refer [link]]
            [moc.ui.common.handlers :refer [pass-to-dispatch]]))

(defn footer []
  [:span
   "Forgotten, or don't have, a password? "
   [link {:path [:url.user/register]} "Get a login link"]])

(defn login [_]
  (dispatch-sync [:login/reset-state])
  (let [loading? (subscribe [:loading?])
        state (subscribe [:login/form-state])
        errors (subscribe [:login/form-errors])]
    (fn [_]
      [:div.login-page {:on-key-up #(when (= 13 (-> % .-keyCode))
                                      (dispatch [:login/send @state]))}
       [:h1.logo "Masters of Cthulhu"]
       [box {:title "Login"
             :footer [footer]}
        [icon-input {:icon "user"
                     :auto-focus true
                     :placeholder "Email"
                     :value (:email @state)
                     :error (:email @errors)
                     :on-change (pass-to-dispatch :login/set-email)}]
        [icon-input {:icon "lock"
                     :type "password"
                     :placeholder "Password"
                     :value (:password @state)
                     :error (:password @errors)
                     :on-change (pass-to-dispatch :login/set-password)}]
        [button {:loading? @loading?
                 :on-click #(dispatch [:login/send @state])}
         "Log in"]]])))
