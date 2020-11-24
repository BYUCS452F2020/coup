(ns coup.db
  (:require
    [crux.api :as crux]
    [clojure.java.io :as io]
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
  (jdbc/execute! ds ["create table if not exists game (
                        game_id int auto_increment primary key,
                        turn int,
                        winner_id int
                        )"])
  (jdbc/execute! ds ["create table if not exists player (
                        player_id int auto_increment primary key,
                        user_id int references user(user_id),
                        game_id int references game(game_id),
                        turn_order int,
                        role_1 varchar(32),
                        role_2 varchar(32),
                        num_coins int
                        )"])
  (jdbc/execute! ds ["create table if not exists deck (
                        deck_id int auto_increment primary key,
                        game_id int references game(game_id),
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

; - - - - - - - - - - - - - - NEW CRUX FUNCTIONS - - - - - - - - - - - - - - -

(def node (crux/start-node {}))

(def GAME_ID_TRACKER "game-id-tracker")

(defn read-entity
  "Reads entity with id."
  [id]
  (crux/entity (crux/db node) (keyword id)))

(defn create-entity
  "Creates entity with id. Overrides if value exists with id."
  [id entity]
  (crux/submit-tx node [[:crux.tx/put (assoc entity :crux.db/id (keyword id))]]))

(defn update-entity-value
  "Updates key in entity with id to value"
  [id key value]
  (let [res (read-entity id)
        without-key (dissoc res (keyword key))
        new-entity (assoc res (keyword key) value)]
    (create-entity id new-entity)))

(defn append-entity-value
  "Updates key in entity with id to value"
  [id key value]
  (let [res (read-entity id)
        old-list ((keyword key) res)
        without-key (dissoc res (keyword key))
        new-entity (assoc res (keyword key) (conj old-list value))]
    (create-entity id new-entity)))


(defn get-current-game-id []
  (:index (read-entity GAME_ID_TRACKER)))

(defn init []
  (if (nil? (read-entity GAME_ID_TRACKER))
      (create-entity GAME_ID_TRACKER {:index 0})))

(defn signup-login-crux
  "If user with username does not exist in database, creates user"
  [username]
  (if (nil? (read-entity username))
      (create-entity username {:games []})))

(defn create-game-crux []
  (let [new-game-tracker (read-entity (:index GAME_ID_TRACKER))]
    (create-entity (str "game_" new-game-tracker) {:turn 0 :winner-id nil :users []})))

(defn create-player-crux [user_id, game_id, turn_order, role_1, role_2]
  (create-entity (str user_id game_id) {:turn_order turn_order :role_1 role_1 :role_2 role_2 :num_coins 0})
  (append-entity-value user_id "games" game_id))

(defn create-deck-crux [game_id, n_am, n_as, n_ca, n_co, n_du]
  (create-entity (str "deck_" game_id) {:num_am n_am :num_as n_as :num_ca n_ca :num_co n_co :num_du n_du}))

(defn get-roles-crux [user_id game_id]
  (let [res (read-entity (str user_id game_id))]
    [(:role_1 res) (:role_2 res)]))

(defn get-user-crux [user-id]
  (read-entity user-id))

(defn get-player-crux [game_id user_id]
  (read-entity (str game_id user_id)))

(defn get-game-crux [game-id]
  (read-entity game-id))

(defn get-user-games-crux [user-id]
  (:games (read-entity user-id)))

(defn get-game-users-crux [game-id]
  (:players (read-entity game-id)))

(defn get-enemy-roles-crux [game-id user-id]
  (let [game (read-entity game-id)
        player-ids (:players game)]
    (flatten (map #(let [player-res (read-entity %)]
                     (list (:role_1 player-res)
                           (:role_2 player-res)))
                  player-ids))))

(defn get-current-turn-player-crux [game_id]
  (:turn (read-entity game_id)))

(defn change-player-coins-crux [game-id user-id num-coins]
  (update-entity-value (str game-id user-id) "coins" num-coins))

(defn kill-influence-crux [game-id user-id role-num]
  (if (= "1" role-num)
    (update-entity-value (str game-id user-id) "role_1" EMPTY-ROLE)
    (update-entity-value (str game-id user-id) "role_2" EMPTY-ROLE)))

(defn set-turn-crux [game-id new-turn]
  (update-entity-value game-id "turn" new-turn))

(defn get-deck-by-game-crux [game-id]
  (read-entity (str "deck_" game-id)))

(defn set-player-role-crux [game-id user-id role-num role]
    (update-entity-value (str game-id user-id) (str "role_" role-num) role))

(defn change-num-role-crux [game-id role n]
  (update-entity-value (str "deck_" game-id) role n))

; - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

; (defn select-all [table]
;   (jdbc/execute! ds
;     [(str "select * from " table)]
;     opt))

; (defn select-something [x]
;   (jdbc/execute! ds
;     ["select * from user where username = ?" x]))
;
; (defn create-game []
;   (strip-id
;     (jdbc/execute! ds
;       ["insert into game (turn) values (0)"]
;       opt)))
;
; (defn create-player [user_id, game_id, turn_order, role_1, role_2]
;   (strip-id
;     (jdbc/execute! ds
;       ["insert into player (user_id, game_id, turn_order, role_1, role_2, num_coins)
;       values (?, ?, ?, ?, ?, 2)" user_id, game_id, turn_order, role_1, role_2]
;       opt)))
;
; (defn create-deck [game_id, n_am, n_as, n_ca, n_co, n_du]
;   (strip-id
;     (jdbc/execute! ds
;       ["insert into deck (game_id, num_am, num_as, num_ca, num_co, num_du)
;       values (?, ?, ?, ?, ?, ?)" game_id n_am n_as n_ca n_co n_du]
;       opt)))

; (defn refresh []
;   (jdbc/execute! ds
;     ["drop table user;
;      drop table player;
;      drop table game;
;      drop table deck"])
;   (init))
;
; (defn get-roles [player_id]
;   (->>
;     (jdbc/execute! ds
;       ["select role_1, role_2
;        from player
;        where player_id = ?" player_id]
;       opt)
;     first
;     vals))
;
; (defn get-user [user-id]
;   (jdbc/execute! ds
;     ["select *
;      from user
;      where user_id = ?" user-id]))
;
; (defn get-user-m [user-id]
;   (first (jdbc/execute! ds
;     ["select *
;      from user
;      where user_id = ?" user-id]
;     opt)))
;
; (defn get-player-m [player_id]
;   (first
;     (jdbc/execute! ds
;       ["select *
;        from player
;        join user on player.user_id = user.user_id
;        where player_id = ?" player_id]
;       opt)))
;
; (defn get-player [player_id]
;   (jdbc/execute! ds
;       ["select *
;        from player
;        join user on player.user_id = user.user_id
;        where player_id = ?" player_id]))
;
; (defn get-game [game-id]
;   (jdbc/execute! ds
;       ["select *
;        from game
;        join deck on game.game_id = deck.game_id
;        where game.game_id = ?" game-id]))
;
; (defn get-game-m [game-id]
;   (first (jdbc/execute! ds
;            ["select *
;             from game
;             join deck on game.game_id = deck.game_id
;             where game.game_id = ?" game-id]
;            opt)))
;
; (defn get-user-players [user-id]
;   (jdbc/execute! ds
;     ["select *
;      from player
;      where player.user_id = ?" user-id]))
;
; (defn get-game-players [game-id]
;   (jdbc/execute! ds
;     ["select *
;      from player
;      where player.game_id = ?" game-id]))
;
; (defn get-game-players-m [game-id]
;   (jdbc/execute! ds
;     ["select *
;      from player
;      where player.game_id = ?" game-id]
;     opt))
;
; (defn get-enemy-roles [player-id]
;   (->> (jdbc/execute! ds
;       ["select role_1, role_2
;        from player
;        where player.game_id =
;        (select game_id
;         from player
;         where player_id = ?)
;        and player.player_id != ?" player-id player-id]
;       opt)
;     (mapcat vals)))

;(get-enemy-roles 1)
;
; (defn get-current-turn-player [game_id]
;   (jdbc/execute! ds
;       ["select player.*
;        from player
;        join game on player.game_id = game.game_id
;        where player.turn_order = game.turn
;        and game.game_id = ?" game_id]))
;
; (defn change-player-coins [player-id num-coins]
;   (jdbc/execute! ds
;     ["update player
;      set num_coins = num_coins + ?
;      where player_id = ?" num-coins player-id]))
;
;
; (defn kill-influence [player-id role-num]
;   (if (= "1" role-num)
;     (jdbc/execute! ds
;       ["update player
;        set role_1 = ?
;        where player_id = ?" EMPTY-ROLE player-id])
;     (jdbc/execute! ds
;       ["update player
;        set role_2 = ?
;        where player_id = ?" EMPTY-ROLE player-id])))
;
; (defn set-turn [game-id new-turn]
;   (jdbc/execute! ds
;     ["update game
;      set turn = ?
;      where game_id = ?" new-turn game-id]))
;
; (defn get-game-by-player-m [player-id]
;   (first
;     (jdbc/execute! ds
;       ["select game.*, count(other_player.*) as num_players
;        from game
;        join player on player.game_id = game.game_id
;        join player as other_player on player.game_id = other_player.game_id
;        where player.player_id = ?" player-id]
;       opt)))
;
; (defn get-game-by-player [player-id]
;   (jdbc/execute! ds
;     ["select game.*, count(other_player.*) as num_players
;      from game
;      join player on player.game_id = game.game_id
;      join player as other_player on player.game_id = other_player.game_id
;      where player.player_id = ?" player-id]))
;
;
; (defn get-deck-by-player-m [player-id]
;   (first (jdbc/execute! ds
;            ["select deck.*
;             from deck
;             join game on game.game_id = deck.game_id
;             join player on player.game_id = game.game_id
;             where player.player_id = ?" player-id]
;            opt)))
;
; (defn set-player-role [player-id role-num role]
;     (jdbc/execute! ds
;       [(str "update player
;        set role_" role-num  " = ?
;        where player_id = ?") role player-id]))
;
;
; (defn change-num-role [deck-id role n]
;   (jdbc/execute! ds
;     [(str "update deck
;      set num_" role " = num_" role " + ?
;      where deck_id = ?") n deck-id]))


#_(defn increase-player-coins [player-id num-coins]
  (jdbc/execute! ds
    ["update player
     set num_coins = num_coins + ?
     where player_id = ?" num-coins player-id]))

#_(defn decrease-player-coins [player-id num-coins]
  (jdbc/execute! ds
    ["update player
     set num_coins = num_coins - ?
     where player_id = ?" num-coins player-id]))

#_(defn get-deck-by-player [player-id]
  (jdbc/execute! ds
    ["select deck.*
     from deck
     join game on game.game_id = deck.game_id
     join player on player.game_id = game.game_id
     where player.player_id = ?" player-id]))

#_(if (= "1" role-num)
    (jdbc/execute! ds
      ["update player
       set role_1 = ?
       where player_id = ?" role-num player-id])
    (jdbc/execute! ds
      ["update player
       set role_2 = ?
       where player_id = ?" role-num player-id]))

#_(defn set-exchange-cards [deck-id role-1 role-2]
  (jdbc/execute! ds
    ["update deck
     set role_1_in_exchange = ?,
         role_2_in_exchange = ?
     where deck_id = ?" role-1 role-2 deck-id]))

#_(defn get-exchange-cards [deck-id]
  (jdbc/execute! ds
    ["select role_1_in_exchange as role_1, role_2_in_exchange as role_2
     from deck
     where deck_id = ?" deck-id]))



#_(defn decrement-num-am [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_am = num_am - 1
     where deck_id = ?" deck-id]))

#_(defn decrement-num-as [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_as = num_as - 1
     where deck_id = ?" deck-id]))

#_(defn decrement-num-ca [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_ca = num_ca - 1
     where deck_id = ?" deck-id]))

#_(defn decrement-num-co [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_co = num_co - 1
     where deck_id = ?" deck-id]))

#_(defn decrement-num-du [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_du = num_du - 1
     where deck_id = ?" deck-id]))

#_(defn increment-num-am [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_am = num_am + 1
     where deck_id = ?" deck-id]))

#_(defn increment-num-as [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_as = num_as + 1
     where deck_id = ?" deck-id]))

#_(defn increment-num-ca [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_ca = num_ca + 1
     where deck_id = ?" deck-id]))

#_(defn increment-num-co [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_co = num_co + 1
     where deck_id = ?" deck-id]))

#_(defn increment-num-du [deck-id]
  (jdbc/execute! ds
    ["update deck
     set num_du = num_du + 1
     where deck_id = ?" deck-id]))
