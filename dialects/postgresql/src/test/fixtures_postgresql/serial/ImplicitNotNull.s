CREATE TABLE test3(
    id SERIAL,
    words TEXT NOT NULL
);

INSERT INTO test3(words)
VALUES ("foo");

CREATE TABLE test4(
    id SMALLSERIAL,
    words TEXT NOT NULL
);

INSERT INTO test4(words)
VALUES ("foo");

CREATE TABLE test5(
    id BIGSERIAL,
    words TEXT NOT NULL
);

INSERT INTO test5(words)
VALUES ("foo");
