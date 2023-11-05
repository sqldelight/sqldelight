SELECT *
FROM CacheTerritoryPoint
-- error[col 39]: <expr> expected, got 'LIKE'
WHERE address LIKE ? OR name  LIKE ? OR  LIKE ?
LIMIT ? OFFSET ?;