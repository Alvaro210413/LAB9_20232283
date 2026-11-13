drop database if exists stf_games_20232283;
create database stf_games_20232283 character set utf8mb4 collate utf8mb4_unicode_ci;
use stf_games_20232283;

create table stf_puzzle_sv (
	stf_game_board_structure varchar(500) null,
    image LONGTEXT null
);

insert into stf_puzzle_sv(stf_game_board_structure,image) VALUES (null,null);