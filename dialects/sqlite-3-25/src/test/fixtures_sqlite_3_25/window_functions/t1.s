CREATE TABLE t1(a INTEGER PRIMARY KEY, b TEXT, c TEXT);
INSERT INTO t1 VALUES   (1, 'A', 'one'  ),
                        (2, 'B', 'two'  ),
                        (3, 'C', 'three'),
                        (4, 'D', 'one'  ),
                        (5, 'E', 'two'  ),
                        (6, 'F', 'three'),
                        (7, 'G', 'one'  );

-- The following SELECT statement returns:
--
--   a | b | group_concat
-------------------------
--   1 | A | A.B
--   2 | B | A.B.C
--   3 | C | B.C.D
--   4 | D | C.D.E
--   5 | E | D.E.F
--   6 | F | E.F.G
--   7 | G | F.G
--
SELECT a, b, group_concat(b, '.') OVER (
  ORDER BY a ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
) AS group_concat FROM t1;

-- The following SELECT statement returns:
--
--   c     | a | b | group_concat
---------------------------------
--   one   | 1 | A | A.D.G
--   one   | 4 | D | D.G
--   one   | 7 | G | G
--   three | 3 | C | C.F
--   three | 6 | F | F
--   two   | 2 | B | B.E
--   two   | 5 | E | E
--
SELECT c, a, b, group_concat(b, '.') OVER (
  PARTITION BY c ORDER BY a RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
) AS group_concat
FROM t1 ORDER BY c, a;

-- The following SELECT statement returns:
--
--   c     | a | b | group_concat
---------------------------------
--   one   | 1 | A | A.D.G
--   two   | 2 | B | B.E
--   three | 3 | C | C.F
--   one   | 4 | D | D.G
--   two   | 5 | E | E
--   three | 6 | F | F
--   one   | 7 | G | G
--
SELECT c, a, b, group_concat(b, '.') OVER (
  PARTITION BY c ORDER BY a RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
) AS group_concat
FROM t1 ORDER BY a;

-- The following SELECT statement returns:
--
--   a | b | c | group_concat
-----------------------------
--   1 | A | one   | A.D.G
--   2 | B | two   | A.D.G.C.F.B.E
--   3 | C | three | A.D.G.C.F
--   4 | D | one   | A.D.G
--   5 | E | two   | A.D.G.C.F.B.E
--   6 | F | three | A.D.G.C.F
--   7 | G | one   | A.D.G
--
SELECT a, b, c,
       group_concat(b, '.') OVER (ORDER BY c) AS group_concat
FROM t1 ORDER BY a;

-- The following SELECT statement returns:
--
--   c     | a | b | group_concat
---------------------------------
--   one   | 1 | A | A.D.G.C.F.B.E
--   one   | 4 | D | D.G.C.F.B.E
--   one   | 7 | G | G.C.F.B.E
--   three | 3 | C | C.F.B.E
--   three | 6 | F | F.B.E
--   two   | 2 | B | B.E
--   two   | 5 | E | E
--
SELECT c, a, b, group_concat(b, '.') OVER (
  ORDER BY c, a ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
) AS group_concat
FROM t1 ORDER BY c, a;

-- The following SELECT statement returns:
--
--   c    | a | b | no_others     | current_row | grp       | ties
--  one   | 1 | A | A.D.G         | D.G         |           | A
--  one   | 4 | D | A.D.G         | A.G         |           | D
--  one   | 7 | G | A.D.G         | A.D         |           | G
--  three | 3 | C | A.D.G.C.F     | A.D.G.F     | A.D.G     | A.D.G.C
--  three | 6 | F | A.D.G.C.F     | A.D.G.C     | A.D.G     | A.D.G.F
--  two   | 2 | B | A.D.G.C.F.B.E | A.D.G.C.F.E | A.D.G.C.F | A.D.G.C.F.B
--  two   | 5 | E | A.D.G.C.F.B.E | A.D.G.C.F.B | A.D.G.C.F | A.D.G.C.F.E
--
SELECT c, a, b,
  group_concat(b, '.') OVER (
    ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE NO OTHERS
  ) AS no_others,
  group_concat(b, '.') OVER (
    ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE CURRENT ROW
  ) AS current_row,
  group_concat(b, '.') OVER (
    ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE GROUP
  ) AS grp,
  group_concat(b, '.') OVER (
    ORDER BY c GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW EXCLUDE TIES
  ) AS ties
FROM t1 ORDER BY c, a;

-- The following SELECT statement returns:
--
--   c     | a | b | group_concat
---------------------------------
--   one   | 1 | A | A
--   two   | 2 | B | A
--   three | 3 | C | A.C
--   one   | 4 | D | A.C.D
--   two   | 5 | E | A.C.D
--   three | 6 | F | A.C.D.F
--   one   | 7 | G | A.C.D.F.G
--
SELECT c, a, b, group_concat(b, '.') FILTER (WHERE c!='two') OVER (
  ORDER BY a
) AS group_concat
FROM t1 ORDER BY a;

-- The following SELECT statement returns:
--
--   b | lead | lag  | first_value | last_value | nth_value_3
-------------------------------------------------------------
--   A | C    | NULL | A           | A          | NULL
--   B | D    | A    | A           | B          | NULL
--   C | E    | B    | A           | C          | C
--   D | F    | C    | A           | D          | C
--   E | G    | D    | A           | E          | C
--   F | n/a  | E    | A           | F          | C
--   G | n/a  | F    | A           | G          | C
--
SELECT b                          AS b,
       lead(b, 2, 'n/a') OVER win AS lead,
       lag(b) OVER win            AS lag,
       first_value(b) OVER win    AS first_value,
       last_value(b) OVER win     AS last_value,
       nth_value(b, 3) OVER win   AS nth_value_3
FROM t1
WINDOW win AS (ORDER BY b ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW);

SELECT group_concat(b, '.') OVER (
  win ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
)
FROM t1
WINDOW win AS (PARTITION BY a ORDER BY c);