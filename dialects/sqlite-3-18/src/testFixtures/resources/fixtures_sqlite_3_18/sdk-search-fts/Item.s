CREATE TABLE item(
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  packageName TEXT NOT NULL,
  className TEXT NOT NULL,
  deprecated INTEGER NOT NULL DEFAULT 0,
  link TEXT NOT NULL,

  UNIQUE (packageName, className)
);

CREATE VIRTUAL TABLE item_index USING fts4(content TEXT);

CREATE TRIGGER populate_index
AFTER INSERT ON item
BEGIN
  INSERT INTO item_index (docid, content)
  VALUES (new.id, new.className);
END;

CREATE TRIGGER update_index
AFTER UPDATE ON item
BEGIN
  UPDATE item_index
  SET content = new.className
  WHERE docid = new.id;
END;

INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)
;

UPDATE item
SET deprecated = ?3,
    link = ?4
WHERE packageName = ?1
  AND className = ?2
;

SELECT COUNT(id)
FROM item
;

SELECT item.*
FROM item
JOIN item_index ON docid = item.id
WHERE item_index MATCH ?1
ORDER BY
  -- deprecated classes are always last
  deprecated ASC,
  rank(matchinfo(item_index)) ASC,
  -- alphabetize to eliminate any remaining non-determinism
  packageName ASC,
  className ASC
LIMIT 50
;
