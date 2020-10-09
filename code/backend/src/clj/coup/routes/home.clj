(ns coup.routes.home
  (:require [coup.layout :as layout]
            [coup.middleware :as middleware]))

(defn home-page [request-map]
  (layout/hiccup-render-cljs-base {:username request-map}))

(def ^{:private true} home-paths
  [
   "/"
   "/article"
   "/admin"])


(defn home-routes
  "The basic routes to be handled by the SPA (as rendered by fn `home-page`)"
  []
  (into [""
         {:middleware [middleware/wrap-base
                       middleware/wrap-formats]}]

        (for [path home-paths]
          [path {:get home-page}])))
