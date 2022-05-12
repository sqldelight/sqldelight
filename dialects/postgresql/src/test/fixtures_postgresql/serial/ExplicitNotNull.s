CREATE TABLE test0(
    id SERIAL NOT NULL,
    words TEXT NOT NULL
);

INSERT INTO test0(words)
VALUES ('foo');

CREATE TABLE test1(
    id SMALLSERIAL NOT NULL,
    words TEXT NOT NULL
);

INSERT INTO test1(words)
VALUES ('foo');

CREATE TABLE test2(
    id BIGSERIAL NOT NULL,
    words TEXT NOT NULL
);

INSERT INTO test2(words)
VALUES ('foo');
