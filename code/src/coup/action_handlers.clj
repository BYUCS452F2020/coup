(ns coup.action-handlers
 (:require
   [coup.db :refer :all]
   [clojure.pprint :refer [pprint]]))

;actions: coup, income, foreign aid, exchange, assassinate, steal, tax
;reactions: block stealing, block assassination, block foreign aid

(defn income
  "Take one coin from the treasury"
  [user-id game-id & args]
  (change-player-coins user-id game-id 1)
  (get-player user-id game-id))

(defn foreign-aid
  "Take two coins from the treasury"
  [user-id game-id & args]
  (if (contains? (set (get-enemy-roles user-id game-id)) "du")
    {:msg "A pesky duke blocked you."}
    (do
      (change-player-coins user-id game-id 2)
      (get-player user-id game-id))))

(def roles [:am :as :ca :co :du])

(defn role-key-to-num [k]
  (->> k
    name
    (str "num-")
    keyword
    ))

;(role-key-to-num :ca)

(defn exchange
  [user-id game-id & args]
  (println "HEY")
  (let [deck (get-deck-by-game game-id)
        _ (prn deck)
        [role1 role2] (->> roles
                        (mapcat (fn [role]
                                  (repeat (role deck) role)))
                        shuffle
                        (take 2))
        player (get-player user-id game-id)
        p-role1 (:role-1 player)
        p-role2 (:role-2 player)
        ;deck-id (:deck-id deck)
        ]
    ;(pprint (sort (merge (get-player-m player-id) deck)))
    (prn [role1 role2 p-role1 p-role2])
    (change-num-role game-id role1 -1)
    (change-num-role game-id role2 -1)
    (change-num-role game-id (keyword p-role1) 1)
    (change-num-role game-id (keyword p-role2) 1)
    (set-player-role user-id game-id 1 (name role1))
    (set-player-role user-id game-id 2 (name role2))
    {:player (get-player user-id game-id)
     :deck (get-deck-by-game game-id)}
    ))


;(exchange [1])

;(get-deck-by-player-m 1)

(defn coup
  [user-id game-id _ target-id role-num & args]
  (if (< (:coins (get-player user-id game-id)) 7)
    {:error "You're too poor"}
    (do
      (change-player-coins user-id game-id (- 7))
      (kill-influence target-id game-id role-num)
      {:killer (get-player user-id game-id)
       :killee (get-player target-id game-id)})))

(defn check-block [target-id game-id role]
  (contains? (set (get-roles target-id game-id)) role))

(defn assassinate
  [user-id game-id _ target-id role-num & args]
  (if (< (:coins (get-player user-id game-id)) 3)
    {:error "You're too poor"}
    (do
      (change-player-coins user-id game-id (- 3))
      (if (check-block target-id game-id "co")
        {:msg "A contessa foiled your assassination."}
        (do
          (kill-influence target-id game-id role-num)
          {:killer (get-player user-id game-id)
           :killee (get-player target-id game-id)})))))

(defn steal
  "Take 2 coins from another player.
  args = [player-id action target-id]"
  [user-id game-id _ target-id & args]
  (if (or (check-block target-id game-id "ca") (check-block target-id game-id "am"))
    {:msg "Your thievery was foiled"}
    (do
      (change-player-coins target-id game-id -2)
      (change-player-coins user-id game-id 2)
      {:stealer (get-player user-id game-id)
       :stealee (get-player target-id game-id)})))


(defn tax
  "Take three coins from the treasury"
  [user-id game-id & args]
  (change-player-coins user-id game-id 3)
  (get-player user-id game-id))


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
        num-am (:deck/num-am deck)
        num-as (:deck/num-as deck)
        num-ca (:deck/num-ca deck)
        num-co (:deck/num-co deck)
        num-du (:deck/num-du deck)
        all-cards (concat
                    (get-list-of-roles num-am "am")
                    (get-list-of-roles num-as "as")
                    (get-list-of-roles num-ca "ca")
                    (get-list-of-roles num-co "co")
                    (get-list-of-roles num-du "du"))
        drawn-cards (take 2 (shuffle all-cards))]
    (set-exchange-cards (:deck/deck-id deck) (get drawn-cards 0) (get drawn-cards 1))
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
        drawn-cards-res (get-exchange-cards (:deck/deck-id deck))
        drawn-cards (list (get drawn-cards-res 0) (get drawn-cards-res 1))
        player-res (first (get-player (get args 0)))
        player-role-1 (:player/role-1 player-res)
        player-role-2 (:player/role-2 player-res)]

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
