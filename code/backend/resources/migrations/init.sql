create table if not exists user (
                        user_id int auto_increment primary key,
                        username varchar(32)
                        )
create table if not exists player (
                        player_id int auto_increment primary key,
                        user_id int,
                        game_id int,
                        turn_order int,
                        role_1 varchar(32),
                        role_2 varchar(32),
                        num_coins int
                        )
create table if not exists game (
                        game_id int auto_increment primary key,
                        turn int,
                        winner_id int
                        )
create table if not exists deck (
                        deck_id int auto_increment primary key,
                        game_id int,
                        num_as int,
                        num_ca int,
                        num_du int,
                        num_co int,
                        num_am int
                        )
