CREATE TABLE logs(
  gateway TEXT NOT NULL,
  text TEXT NOT NULL,
  timestamp TEXT NOT NULL
);

SELECT * FROM logs
  WHERE gateway = :gateway AND ((:skipCommands > 0) OR text NOT LIKE '!%')
  ORDER BY timestamp ASC
  OFFSET (SELECT COUNT(*) FROM logs
      WHERE gateway = :gateway AND ((:skipCommands > 0) OR text NOT LIKE '!%')
) - :amount;