(ns coup.action-handlers
 (:require
   [coup.db :refer :all]
   [clojure.pprint :refer [pprint]]))

;actions: coup, income, foreign aid, exchange, assassinate, steal, tax
;reactions: block stealing, block assassination, block foreign aid

(defn income
  "Take one coin from the treasury"
  [player-id & args]
  (change-player-coins player-id 1)
  (get-player-m player-id))

(defn foreign-aid
  "Take two coins from the treasury"
  [player-id & args]
  (change-player-coins player-id 2)
  (get-player-m player-id))


(def roles [:am :as :ca :co :du])

(defn role-key-to-num [k]
  (->> k
    name
    (str "num_")
    keyword
    ))

;(role-key-to-num :ca)

(defn exchange 
  [player-id & args]
  (let [deck (get-deck-by-player-m player-id)
        drawn (->> roles
               (mapcat (fn [role]
                         (repeat ((role-key-to-num role) deck) role)))
               shuffle
               (take 2))
        role1 (name (first drawn))
        role2 (name (last drawn))
        player (get-player-m player-id)
        p-role1 (:role_1 player)
        p-role2 (:role_2 player)
        deck-id (:deck_id deck)]
    ;(pprint (sort (merge (get-player-m player-id) deck)))
    (change-num-role deck-id role1 -1)
    (change-num-role deck-id role2 -1)
    (change-num-role deck-id p-role1 1)
    (change-num-role deck-id p-role2 1)
    (set-player-role player-id 1 role1)
    (set-player-role player-id 2 role2)
    {:player (get-player-m player-id) 
     :deck (get-deck-by-player-m player-id)}
    ))


;(exchange [1])

;(get-deck-by-player-m 1)

(defn kill
  [player-id target-id role-num cost]
  (if (< (:num_coins (get-player-m player-id)) cost)
    {:error "You're too poor"}
    (do
      (change-player-coins player-id (- cost))
      (kill-influence target-id role-num)
      {:killer (get-player-m player-id)
       :killee (get-player-m target-id)})))

(defn coup
  "Lose 7 coins and force another player to lose an influence
  args = [player-id action target-id role-num]"
  [player-id _ target-id role-num & args]
  (kill player-id target-id role-num 7))

(defn assassinate
  "Lose 3 coins and force another player to lose an influence.
  args = [player-id action target-id role-num]"
  [player-id _ target-id role-num & args]
  (kill player-id target-id role-num 3))

(defn steal
  "Take 2 coins from another player.
  args = [player-id action target-id]"
  [player-id _ target-id & args]
  (change-player-coins target-id -2)
  (change-player-coins player-id 2)
  {:stealer (get-player-m player-id)
   :stealee (get-player-m target-id)})


(defn tax
  "Take three coins from the treasury"
  [player-id & args]
  (change-player-coins player-id 3)
  (get-player-m player-id))



#_(defn get-list-of-roles
  "Returns list of x instances of role"
  [x role]
  (if (<= x 0)
    (list)
    (concat (list role) (get-list-of-roles (- x 1) role))))

#_(defn exchange-draw
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

#_(def decrement-roles-map
  {"am" decrement-num-am
   "as" decrement-num-as
   "ca" decrement-num-ca
   "co" decrement-num-co
   "du" decrement-num-du})

#_(def increment-roles-map
  {"am" increment-num-am
   "as" increment-num-as
   "ca" increment-num-ca
   "co" increment-num-co
   "du" increment-num-du})

#_(defn exchange-choose
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
