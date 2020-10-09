(ns coup.core
  (:require [reagent.core :as r]
            [coup.ajax :refer [load-interceptors!]]
            [coup.routes :as routes]
            [re-frame.core :as rfc]
            [coup.views.core :as views]
            [coup.views.navbar :refer [navbar]])
  (:import goog.History))

;; -------------------------
;; Initialize app

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "nav"))
  (r/render [#'views/main-panel] (.getElementById js/document "app")))

(defn init! []
  (load-interceptors!)
  (routes/init-routes!)
  (rfc/dispatch-sync [:initialize-db])
  (mount-components))
