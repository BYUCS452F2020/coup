(ns coup.env
  (:require 
            [clojure.tools.logging :as log]
            [coup.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[coup started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[coup has shut down successfully]=-"))
   :middleware wrap-dev})
