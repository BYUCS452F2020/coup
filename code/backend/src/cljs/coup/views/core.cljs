 (ns coup.views.core
   (:require [re-frame.core :as re-frame]
             [coup.shared :as shared]
             [coup.events]	
             [coup.subs]))

(defn main-panel []
  (let [view (re-frame/subscribe [:current-view])]
    [@view])) 

(defn render-view [view]
  (shared/err-boundary
   [:div.app-main
    view]))
                                        



