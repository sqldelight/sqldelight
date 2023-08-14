CREATE SEQUENCE integers_01;

CREATE SEQUENCE integers_02 START 31;

CREATE SEQUENCE integers_03 AS INTEGER
    INCREMENT 10
    MINVALUE 100
    MAXVALUE 250000
    START 101
    CACHE 1
    NO CYCLE;

SELECT nextval('integers_01');

SELECT nextval('integers_02');

SELECT nextval('integers_03');
