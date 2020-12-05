(ns coup.back
  (:require
    [coup.db :refer :all]
    [coup.action-handlers :refer :all]
    [coup.info-handlers :refer :all]
    [clojure.pprint :refer [pprint]]))


;Game flow:
; 1: Who's playing?
; 2: Turns

;Turn:
; 1: Make action

;Actions:
; See your cards
; Abilities

;(def roles [:am :as :ca :co :du])

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
  ; (println "received users: " users)
  (let [_ (doall (map signup-login users))
        game-id (create-game users)
        [player-cards deck] (deal (count users))
        player-ids (for [[[i u-id] role] (map list (map-indexed vector users) player-cards)]
                     (apply create-player u-id game-id i (map name role)))]
    (apply create-deck game-id (vals deck))
    {:game-id game-id :player-ids player-ids}))


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
  {:inc #'income
   :aid #'foreign-aid
   :cou #'coup
   :exc #'exchange
   :ass #'assassinate
   :ste #'steal
   :tax #'tax})

(def info-actions
  {"player-info" player-info
   "game-info" game-info
   "user-info" user-info})

; untested
(defn is-turn [user-id game-id]
  (let [{:keys [turn-order]} (get-player user-id game-id)]
    (= turn-order (get-current-turn-player game-id))))

;(defn increment-turn [player-id]
(defn increment-turn [game-id]
  (let [game (get-game game-id)
        ; trash (println game)
        ;game-id (:game-id game)
        current-turn (:turn game)
        num-players (count (:users game))]
    (set-turn game-id (mod (+ current-turn 1) num-players))
    ""))  ; return value will be appended to res for front end


(defn process-info-action
  [args]
  (let [action (get args 0)]
    ((get info-actions action) args)))

(defn process-game-action
  [args]
  (let [;player-id (get  args 0)
        user-id (get args 0)
        game-id (get args 1)
        ; trash (println "player-id: " player-id)
        action (get str-to-action (get args 2))
        ; local-roles (concat (map keyword (get-roles player-id)) [:un])  ; player is restricted to own cards
        local-roles (concat (map keyword roles) [:un])  ; player may lie and use all cards
        acts (set (flatten (vals (select-keys actions local-roles))))]
        ; trash (println acts)]
    (if (not (is-turn user-id game-id))
      "it's not your turn!"
      (if (contains? acts action)
        (let [res (apply (action action-handlers) args)]
          (if (contains? res :error) (println (:error res))
            (do
              (increment-turn game-id)
              res)))
          "Action not allowed. Choose another action."))))

(defn receive-action
  [args]
  (if (> 2 (count args))
    "invalid action, submit another"
    (if (contains? info-actions (get args 0))
      (process-info-action args)
      (process-game-action args))))
