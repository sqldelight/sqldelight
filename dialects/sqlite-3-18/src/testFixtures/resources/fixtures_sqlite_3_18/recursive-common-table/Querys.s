WITH RECURSIVE
  input(sud) AS (
    VALUES('53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79')
  ),
  digits(z, lp) AS (
    VALUES('1', 1)
    UNION ALL SELECT
    CAST(lp+1 AS TEXT), lp+1 FROM digits WHERE lp<9
  ),
  x(s, ind) AS (
    SELECT sud, instr(sud, '.') FROM input
    UNION ALL
    SELECT
      substr(s, 1, ind-1) || z || substr(s, ind+1),
      instr( substr(s, 1, ind-1) || z || substr(s, ind+1), '.' )
     FROM x, digits AS z
    WHERE ind>0
      AND NOT EXISTS (
            SELECT 1
              FROM digits AS lp
             WHERE z.z = substr(s, ((ind-1)/9)*9 + lp.lp, 1)
                OR z.z = substr(s, ((ind-1)%9) + (lp.lp-1)*9 + 1, 1)
                OR z.z = substr(s, (((ind-1)/3) % 3) * 3
                        + ((ind-1)/27) * 27 + lp.lp
                        + ((lp.lp-1) / 3) * 6, 1)
         )
  )
SELECT s FROM x WHERE ind=0;

WITH
  input(sud) AS (
    VALUES('53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79')
  ),
  digits(z, lp) AS (
    VALUES('1', 1)
    UNION ALL SELECT
    CAST(lp+1 AS TEXT), lp+1 FROM digits WHERE lp<9
  ),
  x(s, ind) AS (
    SELECT sud, instr(sud, '.') FROM input
    UNION ALL
    SELECT
      substr(s, 1, ind-1) || z || substr(s, ind+1),
      instr( substr(s, 1, ind-1) || z || substr(s, ind+1), '.' )
     FROM x, digits AS z
    WHERE ind>0
      AND NOT EXISTS (
            SELECT 1
              FROM digits AS lp
             WHERE z.z = substr(s, ((ind-1)/9)*9 + lp.lp, 1)
                OR z.z = substr(s, ((ind-1)%9) + (lp.lp-1)*9 + 1, 1)
                OR z.z = substr(s, (((ind-1)/3) % 3) * 3
                        + ((ind-1)/27) * 27 + lp.lp
                        + ((lp.lp-1) / 3) * 6, 1)
         )
  )
SELECT s FROM x WHERE ind=0;

CREATE TABLE `tbl_accounts` (
  `id` INTEGER,
  `parent_id` INTEGER
);

WITH RECURSIVE `name_tree` AS (
SELECT `id`, `parent_id`
FROM `tbl_accounts`
WHERE `id` = ?
UNION ALL
SELECT `c`.`id`, `c`.`parent_id`
FROM `tbl_accounts` `c`
JOIN `name_tree` `p` ON `c`.`id` = `p`.parent_id
AND `c`.`id` <> `c`.`parent_id`)
SELECT count(*) AS `level` FROM `name_tree`;

CREATE TABLE NodeEntity (
    id TEXT NOT NULL PRIMARY KEY,
    parent_id TEXT
);

WITH RECURSIVE descendants(id, depth) AS (
  SELECT id, 0 AS depth
  FROM NodeEntity
  WHERE id = ?
  UNION ALL
  SELECT n.id, d.depth + 1
  FROM NodeEntity n
  INNER JOIN descendants d ON n.parent_id = d.id
)
SELECT * FROM descendants;

WITH RECURSIVE descendants(id, depth) AS (
  SELECT id, 0 AS depth
  FROM NodeEntity
  WHERE id = ?
  UNION ALL
  SELECT n.id, descendants.depth + 1
  FROM NodeEntity n
  INNER JOIN descendants ON n.parent_id = descendants.id
)
SELECT * FROM descendants;

CREATE TABLE Branch (
    id INTEGER NOT NULL,
    name TEXT NOT NULL,
    parent_id INTEGER NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (parent_id) REFERENCES Branch(id)
);

WITH RECURSIVE ancestor(id, parent_id) AS (
  SELECT id, parent_id
  FROM Branch
  WHERE id = ?
UNION ALL
  SELECT b.id, b.parent_id
  FROM Branch b
  INNER JOIN ancestor a ON a.parent_id = b.id LIMIT 255
)
SELECT * FROM ancestor;
