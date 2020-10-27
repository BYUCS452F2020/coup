(ns coup.action-handlers
 (:require
   [coup.db :refer :all]))

;actions: coup, income, foreign aid, exchange, assassinate, steal, tax
;reactions: block stealing, block assassination, block foreign aid

(defn income
  "Take one coin from the treasury"
  [args]
  (increase-player-coins (get args 0) 1)
  (first (get-player (get args 0))))

(defn foreign-aid
  "Take two coins from the treasury"
  [args]
  (increase-player-coins (get args 0) 2)
  (first (get-player (get args 0))))

(defn coup
  "Lose 7 coins and force another player to lose an influence
  args = [player-id action target-id role-num]"
  [args]
  (decrease-player-coins (get args 0) 7)
  (kill-influence (get args 2) (get args 3))
  (list (first (get-player (get args 0)))
        (first (get-player (get args 2)))))



(defn exchange-draw
  "Draw two influence cards, swap 0 or 1 with current influence cards"
  [args])

(defn exchange-choose
  "Decrement chosen influence by one in deck, increase traded influence by
  one in deck. Set player target influence to chosen influence."
  [args])

(defn assassinate
  "Lose 3 coins and force another player to lose an influence.
  args = [player-id action target-id role-num]"
  [args]
  (decrease-player-coins (get args 0) 3)
  (kill-influence (get args 2) (get args 3))
  (list (first (get-player (get args 0)))
        (first (get-player (get args 2)))))

(defn steal
  "Take 2 coins from another player.
  args = [player-id action target-id]"
  [args]
  (decrease-player-coins (get args 2) 2)
  (increase-player-coins (get args 0) 2)
  (list (first (get-player (get args 0)))
        (first (get-player (get args 2)))))


(defn tax
  "Take three coins from the treasury"
  [args]
  (increase-player-coins (get args 0) 3)
  (first (get-player (get args 0))))
