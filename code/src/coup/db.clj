(ns coup.db
  (:require
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))


(def spec
  {:dbtype "h2"
   :dbname "main"
   :DATABASE_TO_UPPER false})

(def ds (jdbc/get-datasource spec))

(def opt {:return-keys true :builder-fn rs/as-unqualified-lower-maps})

(def EMPTY-ROLE "empty")

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
  (jdbc/execute! ds
      ["select player.*
       from player
       join game on player.game_id = game.game_id
       where player.turn_order = game.turn
       and game.game_id = ?" game_id]))

(defn increase-player-coins [player-id num-coins]
  (jdbc/execute! ds
    ["update player
     set num_coins = num_coins + ?
     where player_id = ?" num-coins player-id]))

(defn decrease-player-coins [player-id num-coins]
  (jdbc/execute! ds
    ["update player
     set num_coins = num_coins - ?
     where player_id = ?" num-coins player-id]))

(defn kill-influence [player-id role-num]
  (if (= "1" role-num)
    (jdbc/execute! ds
      ["update player
       set role_1 = ?
       where player_id = ?" EMPTY-ROLE player-id])
    (jdbc/execute! ds
      ["update player
       set role_2 = ?
       where player_id = ?" EMPTY-ROLE player-id])))
