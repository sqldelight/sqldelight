CREATE TABLE user(name TEXT, phone TEXT);

SELECT DISTINCT user.name
  FROM user, json_each(user.phone)
 WHERE json_each.value LIKE '704-%';

SELECT name FROM user WHERE phone LIKE '704-%'
UNION
SELECT user.name
  FROM user, json_each(user.phone)
 WHERE json_valid(user.phone)
   AND json_each.value LIKE '704-%';

CREATE TABLE big(json TEXT);

SELECT big.rowid, fullkey, value
  FROM big, json_tree(big.json)
 WHERE json_tree.type NOT IN ('object','array');

SELECT big.rowid, fullkey, atom
  FROM big, json_tree(big.json)
 WHERE atom IS NOT NULL;

SELECT DISTINCT json_extract(big.json,'$.id')
  FROM big, json_tree(big.json, '$.partlist')
 WHERE json_tree.key='uuid'
   AND json_tree.value='6fa5181e-5721-11e5-a04e-57f3d7b32808';

   CREATE TABLE IF NOT EXISTS note (
     id              INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
     server_id       TEXT UNIQUE,
     title           TEXT,
     description     TEXT,
     client_id       TEXT,
     mobile_id       TEXT,
     department_id   INTEGER,
     issue_id        TEXT,
     survey_id       TEXT,
     attributes_json TEXT,
     n_template      TEXT,
     source          TEXT,
     source_type     TEXT,
     type            TEXT,
     latitude        REAL,
     longitude       REAL,
     created_at      INTEGER,
     updated_at      INTEGER,
     created_by      TEXT,
     is_synced       INTEGER,
     is_deleted      INTEGER,
     qr_code         TEXT,
     qr_code_current_owner TEXT
   );

   SELECT *, json_extract(attrs_json, path || '.value') AS uidle_since
   FROM (
     SELECT *, note.attributes_json AS attrs_json, json_tree.path AS path
     FROM note AS note, json_tree(note.attributes_json)
     WHERE json_tree.value = 'uidle_since'
     )
   WHERE (uidle_since >= ? AND uidle_since <= ?)
   AND is_deleted = 0;