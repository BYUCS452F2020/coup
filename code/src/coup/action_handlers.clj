(ns coup.action-handlers
 (:require
   [coup.db :refer :all]))

(defn income
  "Take one coin from the treasury"
  [args]
  (increase-player-coins (get args 0) 1)
  (println "new player status: " (get-player (get args 0))))

(defn foreign-aid
  "Take two coins from the treasury"
  [args]
  (increase-player-coins (get args 0) 2)
  (println "new player status: " (get-player (get args 0))))

(defn coup
  "Lose 7 coins and force another player to lose an influence.
  args = [player-id action target-id role-num]"
  [args]
  (decrease-player-coins (get args 0) 7)
  (kill-influence (get args 2) (get args 3))
  (println "new player status: " (get-player (get args 0)))
  (println "new player status: " (get-player (get args 2))))
