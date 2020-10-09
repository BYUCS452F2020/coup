(ns coup.views.dashboard
  (:require [re-frame.core :as rfc]
            [reitit.frontend.easy :as rfe]
            [coup.views.shared-components :as shview]))

(defn home-page []
  [shview/basic-template {:page-class ["home" "dashboard"]
                          :contents 
                          [:div.container
                           [:h1.title "Welcome to coup"]]}])
