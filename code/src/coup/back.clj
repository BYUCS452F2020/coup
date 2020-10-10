(ns coup.back
  (:require
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))


;-------------------------------------------------------------------
; Data Access Layer
;-------------------------------------------------------------------

(def spec
  {:dbtype "h2"
   :dbname "main"
   :DATABASE_TO_UPPER false})

(def ds (jdbc/get-datasource spec))

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
                        num_as int,
                        num_ca int,
                        num_du int,
                        num_co int,
                        num_am int
                        )"]))

(defn create-user [username]
  (jdbc/execute! ds
    ["insert into user(username) values(?)" username]))

(defn select-all []
  (jdbc/execute! ds
    ["select * from user"]
    {:return-keys true :builder-fn rs/as-unqualified-lower-maps}))

(defn select-something [x]
  (jdbc/execute! ds
    ["select * from user where username = ?" x]))

;-------------------------------------------------------------------
; End Data Access Layer
;-------------------------------------------------------------------
