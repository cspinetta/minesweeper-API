create table IF NOT EXISTS player (
  id bigint not null AUTO_INCREMENT,
  username varchar(256) not null,
  created_at timestamp not null,
  deleted_at timestamp,
  PRIMARY KEY (`id`),
  constraint player_username_uindex unique (username)
);

create table IF NOT EXISTS game (
  id bigint not null AUTO_INCREMENT,
  player_id bigint not null,
  state varchar(64) not null,
  start_time timestamp not null,
  finish_time timestamp,
  height int not null,
  width int not null,
  mines int not null,
  created_at timestamp not null,
  deleted_at timestamp,
  PRIMARY KEY (`id`),
  FOREIGN KEY (player_id) REFERENCES player(id)
);

CREATE INDEX IF NOT EXISTS idx_game_player_id ON game (player_id);

create table IF NOT EXISTS cell (
  id bigint not null AUTO_INCREMENT,
  game_id bigint not null,
  x int not null,
  y int not null,
  state varchar(64) not null,
  has_mine tinyint not null,
  has_flag tinyint not null,
  PRIMARY KEY (`id`),
  FOREIGN KEY (game_id) REFERENCES game(id)
);

CREATE INDEX IF NOT EXISTS idx_cell_game_id ON cell (game_id);
