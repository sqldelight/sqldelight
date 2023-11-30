CREATE TABLE test (
  id BIGINT AUTO_INCREMENT
);

CREATE TABLE test2 (
-- error[col 24]: ')', ',', <column constraint real>, ASC, DESC or ON expected, got 'AUTOINCREMENT'
  id BIGINT PRIMARY KEY AUTOINCREMENT
);
