(ns moc.style.core
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]))

(defstyles app
  [:body {:background :white}])