CREATE TABLE person (
  name TEXT NOT NULL
);

INSERT INTO person
--error[col 8]: String literals should use ' instead of "
VALUES ("Veyndan");