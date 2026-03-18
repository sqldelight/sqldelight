CREATE TABLE category(
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT NOT NULL
);

INSERT OR REPLACE
INTO category (rowid, id, name, description)
VALUES (COALESCE((SELECT rowid FROM category WHERE id = ?1), NULL), ?1, ?2, ?3);