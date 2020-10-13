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
                        username varchar(32)
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

(defn signup-login [username]
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

(def roles ["am" "as" "ca" "co" "du"])

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

;(deal 3)

(defn init-game [users]
  (let [user_ids (map signup-login users)
        game_id (create-game)
        [player-cards deck] (deal (count users))]
    (doseq [[[i u_id] role] (map list (map-indexed vector user_ids) player-cards)]
      (create-player u_id game_id i (first role) (last role)))
    (apply create-deck game_id (vals deck))
    ))

#_(defn receive-action [username action]
  (case action))

;(init-game ["test1" "test2" "test3"])

;-------------------------------------------------------------------
; End Services Layer
;-------------------------------------------------------------------
