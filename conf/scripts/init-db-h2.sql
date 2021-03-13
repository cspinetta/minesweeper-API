create table player (
  id bigint not null AUTO_INCREMENT,
  username varchar(256) not null,
  created_timestamp timestamp not null,
  deleted_timestamp timestamp,
  PRIMARY KEY (`id`),
  constraint player_username_uindex unique (username)
);
