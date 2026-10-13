SELECT *
FROM CacheTerritoryPoint
-- error[col 39]: <expr> expected, got 'BETWEEN'
WHERE address LIKE ? OR name  LIKE ? OR  BETWEEN ?
LIMIT ? OFFSET ?;
