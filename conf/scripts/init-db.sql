CREATE TABLE IF NOT EXISTS player (
  id serial primary key,
  username varchar(256) not null,
  encoded_pass varchar(256) not null,
  created_at timestamp not null,
  deleted_at timestamp,
  UNIQUE(username)
);

CREATE TABLE IF NOT EXISTS game (
  id SERIAL PRIMARY KEY,
  player_id int not null,
  state varchar(64) not null,
  start_time timestamp not null,
  finish_time timestamp,
  last_start_to_play timestamp not null,
  total_time_seconds bigint not null,
  height int not null,
  width int not null,
  mines int not null,
  created_at timestamp not null,
  deleted_at timestamp,
  CONSTRAINT fk_player
      FOREIGN KEY(player_id)
	  REFERENCES player(id)
);

-- CREATE INDEX IF NOT EXISTS idx_game_player_id ON game USING HASH (player_id);

CREATE TABLE IF NOT EXISTS cell (
  id SERIAL PRIMARY KEY,
  game_id int not null,
  x int not null,
  y int not null,
  state varchar(64) not null,
  has_mine boolean not null,
  adjacent_mines int not null,
  CONSTRAINT fk_game
      FOREIGN KEY(game_id)
	  REFERENCES game(id)
);

-- CREATE INDEX IF NOT EXISTS idx_cell_game_id ON cell USING HASH (game_id);
