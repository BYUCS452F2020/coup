(ns coup.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[coup started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[coup has shut down successfully]=-"))
   :middleware identity})
