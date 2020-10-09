(ns coup.handler
  (:require
            [coup.layout :refer [error-page]]
            [coup.routes.home :refer [home-routes]]
            [reitit.ring :as ring]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [coup.env :refer [defaults]]
            [mount.core :as mount]
            [coup.routes.services :refer [service-routes]]
            [coup.db.core :as db]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (do
    (println "Connecting to db")
    (db/init)
    (println "Starting ring-handler")
    (ring/ring-handler
     (ring/router
      [(home-routes)
       (service-routes)])

     (ring/routes
      (ring/create-resource-handler
       {:path "/"})
      ;; (wrap-content-type
      (wrap-webjars (constantly nil))
      (ring/create-default-handler
       {:not-found #(do (println "Not found:" (prn-str %))
                        (error-page {:status 404, :title "404 - Page not found"}))
        :method-not-allowed
        (constantly (error-page {:status 405, :title "405 - Not allowed"}))
        :not-acceptable
        (constantly (error-page {:status 406, :title "406 - Not acceptable"}))})))))
