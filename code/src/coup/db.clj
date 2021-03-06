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

; (defn signup-login
;   "If user with username does not exist in database, creates user"
;   [username]
;   (println "signing up users")
;   (let [user_id (:user_id (first (jdbc/execute! ds
;                                    ["select user_id from user
;                                      where username = ?" username]
;                                    {:return-keys true :builder-fn rs/as-unqualified-lower-maps})))]
;
;     (if user_id
;       user_id
;       (:user_id (first (jdbc/execute! ds
;                          ["insert into user (username) values (?)" username]
;                          {:return-keys true :builder-fn rs/as-unqualified-lower-maps}))))))

; - - - - - - - - - - - - - - NEW CRUX FUNCTIONS - - - - - - - - - - - - - - -

(defonce node (crux/start-node {}))

(def GAME-ID-TRACKER :game-id-tracker)

#_(defn read-entity
  "Reads entity with id."
  [id]
  (if (nil? id)
    nil
    (crux/entity (crux/db node) (keyword id))))



(defn create-entity
  "Creates entity with id. Overrides if value exists with id."
  [id entity]
  ; (println "in create-entity with" id)
  (crux/await-tx node (crux/submit-tx node [[:crux.tx/put (assoc entity :crux.db/id id)]]))
  id)

#_(defn assoc-entity-value
  "Updates key in entity with id to value"
  [id key value]
  (let [res (crux/entity (crux/db node) id)
        without-key (dissoc res (keyword key))
        new-entity (assoc res (keyword key) value)]
    (create-entity id new-entity)))

(defn assoc-entity-value
  [id k v]
  (crux/await-tx node (crux/submit-tx node [[:crux.tx/put (assoc (crux/entity (crux/db node) id) k v)]])))

(defn update-entity-value
  [id k f & args]
  (crux/await-tx node (crux/submit-tx node [[:crux.tx/put (apply update (crux/entity (crux/db node) id) k f args)]])))

#_(defn append-entity-value
  "Updates key in entity with id to value"
  [id key value]
  (let [res (crux/entity (crux/db node) id)
        old-list ((keyword key) res)
        without-key (dissoc res (keyword key))
        new-entity (assoc res (keyword key) (conj old-list value))]
    (create-entity id new-entity)))

#_(defn get-current-game-id []
  (:index (crux/entity (crux/db node) GAME-ID-TRACKER)))

(defn init []
  (if (nil? (crux/entity (crux/db node) GAME-ID-TRACKER))
      (create-entity GAME-ID-TRACKER {:index 0})))

(defn signup-login
  "If user with username does not exist in database, creates user"
  [username]
  ; (println "in signup-login with" username)
  (if (nil? (crux/entity (crux/db node) {:user username}))
      (create-entity {:user username} {:games []})))

(defn create-game [users]
  (let [new-game-tracker (:index (crux/entity (crux/db node) GAME-ID-TRACKER))]
        ; trash (println "tracker:" new-game-tracker)]
    (create-entity {:game (str new-game-tracker)} {:turn 0 :winner-id nil :users users})
    (str new-game-tracker)))

(defn create-player [user-id, game-id, turn-order, role-1, role-2]
  (create-entity {:user user-id :game (str game-id)} {:turn-order turn-order :role-1 role-1 :role-2 role-2 :coins 0})
  (println "created player:" (str user-id " " game-id))
  (update-entity-value {:user user-id} :games conj (str game-id)))

(defn create-deck [game-id, n-am, n-as, n-ca, n-co, n-du]
  (create-entity {:deck (str game-id)} {:am n-am :as n-as :ca n-ca :co n-co :du n-du}))

(defn get-roles [user-id game-id]
  (let [res (crux/entity (crux/db node) {:user user-id :game game-id})]
    [(:role-1 res) (:role-2 res)]))

(defn get-user [user-id]
  (crux/entity (crux/db node) {:user user-id}))

(defn get-player [user-id game-id]
  (crux/entity (crux/db node) {:user user-id :game game-id}))

;(get-player "user1""game-0")

(defn get-game [game-id]
  (crux/entity (crux/db node) {:game game-id}))

#_(defn get-user-games [user-id]
  (:games (crux/entity (crux/db node) user-id)))

#_(defn get-game-users [game-id]
  (:players (crux/entity (crux/db node) game-id)))


(defn get-enemy-roles [user-id game-id]
  (let [game (crux/entity (crux/db node) {:game game-id})
        player-ids (:users game)]
    (mapcat #((juxt :role-1 :role-2)
              (crux/entity (crux/db node)
                {:game game-id :user %}))
      (remove #{user-id} player-ids))))

(defn get-current-turn-player [game-id]
  (:turn (crux/entity (crux/db node) {:game game-id})))

(defn change-player-coins [user-id game-id num-coins]
  (update-entity-value {:user user-id :game game-id} :coins + num-coins))

(defn kill-influence [user-id game-id role-num]
  (if (= "1" role-num)
    (assoc-entity-value {:user user-id :game game-id} :role-1 EMPTY-ROLE)
    (assoc-entity-value {:user user-id :game game-id} :role-2 EMPTY-ROLE)))

(defn set-turn [game-id new-turn]
  (assoc-entity-value {:game game-id} :turn new-turn))

(defn get-deck-by-game [game-id]
  (crux/entity (crux/db node) {:deck game-id}))

(defn set-player-role [user-id game-id role-num role]
    (assoc-entity-value {:user user-id :game game-id} (keyword (str "role-" role-num)) role))

(defn change-num-role [game-id role n]
  (update-entity-value {:deck game-id} role + n))

(defn get-game-players [args]
  "get-game-players needs to be implemented in db")

(defn get-user-players [args]
  "get-user-players need to be implemented in db")


(require '[clojure.pprint :refer [pprint]])

(comment
  (pprint
    (crux/q (crux/db node)
      {:find '[doc]
       :full-results? true
       :where '[[doc :crux.db/id]]}))
  (crux/submit-tx node
    [[:crux.tx/put 
      {:turn-order 1,
       :role-1 "am",
       :role-2 "ca",
       :coins 1,
       :crux.db/id {:user "user2", :game "0"}}
      ]])

  )

#_(pprint
  (map first
    (crux/q (crux/db node)
      {:find '[doc]
       :full-results? true
       :where '[[doc :crux.db/id]]})))




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

;
; #_(defn increase-player-coins [player-id num-coins]
;   (jdbc/execute! ds
;     ["update player
;      set num_coins = num_coins + ?
;      where player_id = ?" num-coins player-id]))
;
; #_(defn decrease-player-coins [player-id num-coins]
;   (jdbc/execute! ds
;     ["update player
;      set num_coins = num_coins - ?
;      where player_id = ?" num-coins player-id]))
;
; #_(defn get-deck-by-player [player-id]
;   (jdbc/execute! ds
;     ["select deck.*
;      from deck
;      join game on game.game_id = deck.game_id
;      join player on player.game_id = game.game_id
;      where player.player_id = ?" player-id]))
;
; #_(if (= "1" role-num)
;     (jdbc/execute! ds
;       ["update player
;        set role_1 = ?
;        where player_id = ?" role-num player-id])
;     (jdbc/execute! ds
;       ["update player
;        set role_2 = ?
;        where player_id = ?" role-num player-id]))
;
; #_(defn set-exchange-cards [deck-id role-1 role-2]
;   (jdbc/execute! ds
;     ["update deck
;      set role_1_in_exchange = ?,
;          role_2_in_exchange = ?
;      where deck_id = ?" role-1 role-2 deck-id]))
;
; #_(defn get-exchange-cards [deck-id]
;   (jdbc/execute! ds
;     ["select role_1_in_exchange as role_1, role_2_in_exchange as role_2
;      from deck
;      where deck_id = ?" deck-id]))
;
;
;
; #_(defn decrement-num-am [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_am = num_am - 1
;      where deck_id = ?" deck-id]))
;
; #_(defn decrement-num-as [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_as = num_as - 1
;      where deck_id = ?" deck-id]))
;
; #_(defn decrement-num-ca [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_ca = num_ca - 1
;      where deck_id = ?" deck-id]))
;
; #_(defn decrement-num-co [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_co = num_co - 1
;      where deck_id = ?" deck-id]))
;
; #_(defn decrement-num-du [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_du = num_du - 1
;      where deck_id = ?" deck-id]))
;
; #_(defn increment-num-am [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_am = num_am + 1
;      where deck_id = ?" deck-id]))
;
; #_(defn increment-num-as [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_as = num_as + 1
;      where deck_id = ?" deck-id]))
;
; #_(defn increment-num-ca [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_ca = num_ca + 1
;      where deck_id = ?" deck-id]))
;
; #_(defn increment-num-co [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_co = num_co + 1
;      where deck_id = ?" deck-id]))
;
; #_(defn increment-num-du [deck-id]
;   (jdbc/execute! ds
;     ["update deck
;      set num_du = num_du + 1
;      where deck_id = ?" deck-id]))
