(ns coup.back
  (:require
    [coup.db :refer :all]
    [coup.action-handlers :refer :all]
    [clojure.pprint :refer [pprint]]))


;-------------------------------------------------------------------
; Data Access Layer
;-------------------------------------------------------------------


;(get-roles 1)
;(select-all "player")
;(refresh)
;(create-game)
;(select-all)
;(signup-login "test3")
;-------------------------------------------------------------------
; End Data Access Layer
;-------------------------------------------------------------------

;-------------------------------------------------------------------
; Begin Services Layer
;-------------------------------------------------------------------

;Game flow:
; 1: Who's playing?
; 2: Turns

;Turn:
; 1: Make action

;Actions:
; See your cards
; Abilities

(def roles [:am :as :ca :co :du])

(defn deal [n-players]
  (let [deck (->> roles
               (mapcat (fn [role]
                         (repeat 3 role)))
               shuffle)

        player-cards (->> deck
                       (partition 2)
                       (take n-players))
        deck (->> deck
               (drop (* 2 n-players))
               frequencies
               (merge (reduce (fn [x y] (merge x {y 0})) {} roles)))]

    [player-cards deck]))


(defn init-game [users]
  (println "recieved users: " users)
  (let [user_ids (doall (map signup-login users))
        game_id (create-game)
        [player-cards deck] (deal (count users))
        player_ids (for [[[i u_id] role] (map list (map-indexed vector user_ids) player-cards)]
                     (apply create-player u_id game_id i (map name role)))]
    (apply create-deck game_id (vals deck))
    {:game_id game_id :player_ids player_ids}))


(defn get-player-id [username]
  (let [res (get-player-by-username username)]
    (if (empty? res)
        ""
        (:player-id (first res)))))

;actions: coup, income, foreign aid, exchange, assassinate, steal, tax
;reactions: block stealing, block assassination, block foreign aid

(def actions {:un [:cou :inc :aid]
              :am [:exc :bls]
              :as [:ass]
              :ca [:ste :bls]
              :co [:bla]
              :du [:tax :blf]})  ; blf == block foreign-aid

(def str-to-action
  {"coup" :cou
   "income" :inc
   "foreign-aid" :aid
   "exchange" :exc
   "assassinate" :ass
   "steal" :ste
   "tax" :tax  ; is this the Duke's action of taking 3 coins?
   "block-stealing" :bls
   "block-assassination" :bla
   "block-foreign-aid" :blf})

(def action-handlers
  {:inc income
   :aid foreign-aid
   :cou coup
   :exc exchange-draw
   :ass assassinate
   :ste steal
   :tax tax})

; untested
(defn is-turn [player_id]
  (let [player_res (get-player player_id)]
    (if (= 0 (count player_res))
      false
      (do
       (let [current-turn-player (first (get-current-turn-player (:player/game_id (first player_res))))
             current-turn-player-id (:player/player_id current-turn-player)]
         (= (str player_id) (str current-turn-player-id)))))))

(defn receive-action
  [args]
  (if (> 2 (count args))
    (println "invalid action, submit another")
    (let [player_id (get args 0)
          trash (println "player_id: " player_id)
          action (get str-to-action (get args 1))
          ; roles (concat (map keyword (get-roles player_id)) [:un])
          local-roles (concat (map keyword roles) [:un])
          acts (set (flatten (vals (select-keys actions local-roles))))
          trash (println acts)]
      (if (not (is-turn player_id))
        (println "it's not your turn!")
        (if (contains? acts action)
          ((action action-handlers) args)
          (println "we can't do that"))))))



;(keyword "stiff")
;(concat (map key (get-roles 1)) [:un])
;(receive-action 1 :bla)
;(refresh)
;(init-game ["test1" "test2" "test3"])
;(pprint (select-all "player"))

;-------------------------------------------------------------------
; End Services Layer
;-------------------------------------------------------------------
