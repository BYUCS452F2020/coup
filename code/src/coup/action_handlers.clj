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

; (def roles [:am :as :ca :co :du])

(defn get-list-of-roles
  "Returns list of x instances of role"
  [x role]
  (if (<= x 0)
    (list)
    (concat (list role) (get-list-of-roles (- x 1) role))))

(defn exchange-draw
  "Draw two influence cards"
  [args]
  (let [deck (first (get-deck-by-player (get args 0)))
        num-am (:deck/num_am deck)
        num-as (:deck/num_as deck)
        num-ca (:deck/num_ca deck)
        num-co (:deck/num_co deck)
        num-du (:deck/num_du deck)
        all-cards (concat
                    (get-list-of-roles num-am "am")
                    (get-list-of-roles num-as "as")
                    (get-list-of-roles num-ca "ca")
                    (get-list-of-roles num-co "co")
                    (get-list-of-roles num-du "du"))
        drawn-cards (take 2 (shuffle all-cards))]
    (set-exchange-cards (:deck/deck_id deck) (get drawn-cards 0) (get drawn-cards 1))
    drawn-cards))

(def decrement-roles-map
  {"am" decrement-num-am
   "as" decrement-num-as
   "ca" decrement-num-ca
   "co" decrement-num-co
   "du" decrement-num-du})

(def increment-roles-map
  {"am" increment-num-am
   "as" increment-num-as
   "ca" increment-num-ca
   "co" increment-num-co
   "du" increment-num-du})

(defn exchange-choose
  "Decrement chosen influence by one in deck, increase traded influence by
  one in deck. Set player target influence to chosen influence.
  args = [player-id action chosen-influence role-num-to-replace (chosen-influence-2 role-num-to-replace-2)]"
  [args]
  ; Get cards in exchange
  (let [deck (first (get-deck-by-player (get args 0)))
        drawn-cards-res (get-exchange-cards (:deck/deck_id deck))
        drawn-cards (list (get drawn-cards-res 0) (get drawn-cards-res 1))
        player-res (first (get-player (get args 0)))
        player-role-1 (:player/role_1 player-res)
        player-role-2 (:player/role_2 player-res)]

    ; Process first exchange
    (if (not (contains? drawn-cards))
      "Invalid choice. Choose from the cards you drew."
      (do
        ; decrement role in deck
        ((get decrement-roles-map (get args 2)))
        (if (= 1 (get args 3))
          (do
            ; increment traded role in deck
            ((get increment-roles-map player-role-1))
            ; set new player role
            (set-player-role (get args 0) "1"))
          (do
            ; increment traded role in deck
            ((get increment-roles-map player-role-2))
            ; set new player role
            (set-player-role (get args 0) "2")))))))
    ; Process second exchange


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
