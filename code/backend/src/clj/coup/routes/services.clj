(ns coup.routes.services
  (:require
    [coup.config :refer [env]]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [coup.middleware.formats :as formats]
    [coup.middleware.exception :as exception]
    [coup.middleware :as middleware]
    [coup.routes.service-handlers.handlers :as service-handlers]
    [ring.util.http-response :as response]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.partial-content :refer [wrap-partial-content]]
    [coup.db.core :as db]))

(defn service-routes []
   ["/api"
    {:coercion spec-coercion/coercion
     :muuntaja formats/instance
     :swagger {:id ::api}
     :middleware [;; query-params & form-params
                  parameters/parameters-middleware
                  ;; resource-negotiation
                  muuntaja/format-negotiate-middleware
                  ;; encoding response body
                  muuntaja/format-response-middleware
                  ;; exception handling
                  exception/exception-middleware
                  ;; decoding request body
                  muuntaja/format-request-middleware
                  ;; coercing response bodys
                  coercion/coerce-response-middleware
                  ;; coercing request parameters
                  coercion/coerce-request-middleware
                  ;; multipart
                  multipart/multipart-middleware
                  wrap-multipart-params]}

    ;; swagger documentation
    ["" {:no-doc true
         :swagger {:info {:title "Coup"
                          :version "beta"
                          :description "Back End Service for Coup Card Game"}}}
     ["/swagger.json"
      {:get (swagger/create-swagger-handler)}]

     ["/docs/*"
      {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]
     ["/docs"
      {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]

     ["/ping"
      {:get {:summary "ping, requires valid session-id"
             :responses {200 {:body {:message string?}}}
             :handler (fn [req]
                        {:status 200
                         :body {:message "pong"}})}}]]
    [""
     {:swagger {:tags ["default"]}}
     ["/create-user"
      {:post {:summary "puts a user into the database"
              :parameters {:body {:username string?}}
              :responses {200 {:body {:message string?}}}
              :handler (fn [{{{:keys [username]} :body} :parameters}]
                         (db/create-user username)
                         {:status 200
                          :body {:message "success?"}})}}]
     ["/get-all-users"
      {:get {:summary "reads all users from the database"
              ; :parameters {:body {:data string?}}
              :responses {200 {:body {:usernames [string?]}}}
              :handler (fn [req]
                         {:status 200
                          :body {:usernames (map #(:username %) (db/select-all))}})}}]]])
