(ns coup.core
  (:require
    [coup.interface :refer :all]
    [coup.back :refer :all]
    [nrepl.server :as nrepl]))

(defn -main []
  (nrepl/start-server :port 7888)
  (println "repl started")
  (init)
  (run-cli))
