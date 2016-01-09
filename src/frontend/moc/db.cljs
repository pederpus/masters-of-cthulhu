(ns moc.db
  (:require [re-frame.core :refer [register-handler]]))

(def initial-state {:loading-count 0
                    :ui {:user/login {:email ""
                                      :password ""
                                      :errors {}}
                         :user/register {:email ""
                                         :success? false
                                         :errors {}}}})

(register-handler
 :app/initialize
 (fn [_ _]
   initial-state))