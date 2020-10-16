(ns coup.deck-definition
  (:require))

(defn get-num-cards [num-players]
  (if (< num-players 7)
    3  ; 3-6 players
    (if (< num-players 9)
      4  ; 7-8 players
      5)))  ; 9-10 -players
