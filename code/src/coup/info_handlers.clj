(ns coup.info-handlers
 (:require
   [coup.db :refer :all]))

(defn player-info
  "Returns info about target player"
  [args]
  (first (get-player (get args 1))))

(defn game-info
  "Returns info about target game"
  [args]
  (first (get-game (get args 1))))

(defn user-info
  "Returns info about target user"
  [args]
  (concat
    (first (get-user (get args 1)))
    (get-user-players (get args 1))))
