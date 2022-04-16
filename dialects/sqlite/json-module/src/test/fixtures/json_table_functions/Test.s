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