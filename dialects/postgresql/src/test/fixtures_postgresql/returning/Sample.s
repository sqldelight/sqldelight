CREATE TABLE account(
    id      SERIAL PRIMARY KEY,
    name    TEXT NOT NULL,
    balance INT  NOT NULL
);

INSERT INTO account(name, balance)
VALUES ('Jane Doe', 0), ('John Doe', 0)
RETURNING id;

UPDATE account
SET balance = 100
WHERE name = 'Jane Doe'
RETURNING *;

DELETE FROM account
WHERE name = 'Jane Doe'
RETURNING id, name;
