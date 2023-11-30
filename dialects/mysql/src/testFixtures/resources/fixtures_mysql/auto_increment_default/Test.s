CREATE TABLE user(
  id integer NOT NULL PRIMARY KEY AUTO_INCREMENT,
  username varchar(16) NOT NULL,
  firstname varchar(24) NOT NULL,
  lastname varchar(32) NOT NULL
);

INSERT INTO user(username, firstname, lastname)
VALUES (?, ?, ?);