(ns coup.back
  (:require
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [clojure.pprint :refer [pprint]]))


;-------------------------------------------------------------------
; Data Access Layer
;-------------------------------------------------------------------

(def spec
  {:dbtype "h2"
   :dbname "main"
   :DATABASE_TO_UPPER false})

(def ds (jdbc/get-datasource spec))

(def opt {:return-keys true :builder-fn rs/as-unqualified-lower-maps})

(defn init []
  (jdbc/execute! ds ["create table if not exists user (
                        user_id int auto_increment primary key,
                        username varchar(32) unique
                        )"])
  (jdbc/execute! ds ["create table if not exists player (
                        player_id int auto_increment primary key,
                        user_id int,
                        game_id int,
                        turn_order int,
                        role_1 varchar(32),
                        role_2 varchar(32),
                        num_coins int
                        )"])
  (jdbc/execute! ds ["create table if not exists game (
                        game_id int auto_increment primary key,
                        turn int,
                        winner_id int
                        )"])
  (jdbc/execute! ds ["create table if not exists deck (
                        deck_id int auto_increment primary key,
                        game_id int,
                        num_am int,
                        num_as int,
                        num_ca int,
                        num_co int,
                        num_du int
                        )"]))

(defn strip-id [thing]
  (first (vals (first thing))))

(defn signup-login
  "If user with username does not exist in database, creates user"
  [username]
  (let [user_id (:user_id (first (jdbc/execute! ds
                                   ["select user_id from user
                                     where username = ?" username]
                                   {:return-keys true :builder-fn rs/as-unqualified-lower-maps})))]

    (if user_id
      user_id
      (:user_id (first (jdbc/execute! ds
        ["insert into user (username) values (?)" username]
        {:return-keys true :builder-fn rs/as-unqualified-lower-maps}))))))

(defn select-all [table]
  (jdbc/execute! ds
    [(str "select * from " table)]
    {:return-keys true :builder-fn rs/as-unqualified-lower-maps}))

(defn select-something [x]
  (jdbc/execute! ds
    ["select * from user where username = ?" x]))

(defn create-game []
  (strip-id
    (jdbc/execute! ds
      ["insert into game (turn) values (0)"]
      opt)))

(defn create-player [user_id, game_id, turn_order, role_1, role_2]
  (strip-id
    (jdbc/execute! ds
      ["insert into player (user_id, game_id, turn_order, role_1, role_2, num_coins)
      values (?, ?, ?, ?, ?, 2)" user_id, game_id, turn_order, role_1, role_2]
      opt)))

(defn create-deck [game_id, n_am, n_as, n_ca, n_co, n_du]
  (strip-id
    (jdbc/execute! ds
      ["insert into deck (game_id, num_am, num_as, num_ca, num_co, num_du)
      values (?, ?, ?, ?, ?, ?)" game_id n_am n_as n_ca n_co n_du]
      opt)))

(defn refresh []
  (jdbc/execute! ds
    ["drop table user;
     drop table player;
     drop table game;
     drop table deck"])
  (init))

(defn get-roles [player_id]
  (->>
    (jdbc/execute! ds
      ["select role_1, role_2
       from player
       where player_id = ?" player_id]
      opt)
    first
    vals))

(defn get-player [player_id]
  (jdbc/execute! ds
      ["select *
       from player
       where player_id = ?" player_id]))

(defn get-player-by-username [username]
  (jdbc/execute! ds
      ["select *
       from player
       where username = ?" username]))

(defn get-current-turn-player [game_id]
  (println "in get-current-turn-player with " game_id)
  (jdbc/execute! ds
      ["select player.*
       from player
       join game on player.game_id = game.game_id
       where player.turn_order = game.turn
       and game.game_id = ?" game_id]))

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
               (merge (reduce (fn [x y] (merge x {y 0})) {} roles))
               )]
  [player-cards deck]))


(defn init-game [users]
  (println "recieved users: " users)
  (let [user_ids (doall (map signup-login users))
        game_id (create-game)
        [player-cards deck] (deal (count users))
        player_ids (for [[[i u_id] role] (map list (map-indexed vector user_ids) player-cards)]
                     (apply create-player u_id game_id i (map name role)))]
    (apply create-deck game_id (vals deck))
    {:game_id game_id :player_ids player_ids}
    ))

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
              :co [:bla]})

(def str-to-action
  {"coup" :cou
   "income" :inc
   "foreign-aid" :aid
   "exchange" :exc
   "assassinate" :ass
   "steal" :ste
   "tax" :tax
   "block-stealing" :bls
   "block-assassination" :bla
   "block-foreign-aid" :blf})


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
  ([player_id action]
   (let [roles (concat (map keyword (get-roles player_id)) [:un])
         acts (set (flatten (vals (select-keys actions roles))))
         trash (println acts)]
     (if (not (is-turn player_id))
            (println "it's not your turn!")
            (if (contains? acts action)
              (println "we can do that")
              (println "we can't do that")))))
   ([args]  ; must have 2 elements in args: player-id and action string
    (if (not (= 2 (count args)))
        (println "invalid command")
        (receive-action (get args 0) (str-to-action (get args 1))))))



;(keyword "stiff")
;(concat (map key (get-roles 1)) [:un])
;(receive-action 1 :bla)
;(refresh)
;(init-game ["test1" "test2" "test3"])
;(pprint (select-all "player"))

;-------------------------------------------------------------------
; End Services Layer
;-------------------------------------------------------------------
