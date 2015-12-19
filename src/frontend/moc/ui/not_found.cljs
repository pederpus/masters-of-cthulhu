(ns moc.ui.not-found
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [moc.router :refer [router]]
            [moc.util :as util]))

(defui NotFound
  Object
  (render [this]
    (dom/div nil "Page not found!")))

(def not-found (om/factory NotFound))

(defmethod router :default [_]
  (util/render (not-found)))