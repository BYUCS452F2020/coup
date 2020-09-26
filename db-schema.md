# Schema:

User (<ins>user_id</ins>, username)

Player (<ins>player_id</ins>, user_id, game_id, turn_order, role_1, role_2, num_coins)
    Foreign Key user_id references User
    Foreign Key game_id references Game

Game (<ins>game_id</ins>, turn, winner_id)
  Foreign Key winner_id references Player

Deck (<ins>deck_id</ins>, game_id, num_assassins, num_captains, num_dukes, num_contessas, num_ambassadors, num_coins)
    Foreign Key game_id references Game


# Commentary:

### 1. the name of each table

see schema

### 2 & 3. brief explanation of each table name and what kind of entity each table represents

The User table represents users in the system, not the game itself. This table just stores the user's username (no authentication required to play).

The Player table represents a user in a specific game. This table stores information specific to a game instance about a player, such as how many coins they have.

The Game table represents a single game (1 round only). It tracks game specific data (like who's turn it is).

The Deck table represents the materials shared by all players in a game (i.e. undrawn cards and unclaimed coins).

### 4. how each table relates to other entities/tables

Users are independent. They do not rely on any other tables.

Players each track which user they represent (via user_id) and in which game they are participating (via game_id). This means that a user may take part in multiple games at once. For each new game he/she joins, a new Player is created.

Games each track who the winning player is. This value is null until a winner is determined.

Decks each track to which Game they belong.

### 5. evidence of normalization

Each table represents a single entity

### 6. all column names given

see schema

### 7. key columns identified

see schema

### 8. any foreign keys identified

see schema

### 9. brief explanation of column names present

User.username - the username that identifies each user

Player.turn_order - a number indicating where the player falls in the turn order

Player.role_1 - each player has 2 roles at a time, each being one of {assassin, captain, duke, contessa, ambassador}

Player.role_2 - ^^

Player.num_coins - the number of coins a player has at his/her disposal

Game.turn - which player has the current turn

Deck.num_assassins - the number of assassins left in the deck

Deck.num_captains - the number of captains left in the deck

Deck.num_dukes - the number of dukes left in the deck

Deck.num_contessas - the number of contessas left in the deck

Deck.num_ambassadors - the number of ambassadors left in the deck

Deck.num_coins - the number of coins left in the deck
