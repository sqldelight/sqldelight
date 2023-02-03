CREATE TABLE uuid_test (
  pk uuid
);

SELECT gen_random_uuid(), pk FROM uuid_test;

INSERT INTO uuid_test(pk) SELECT gen_random_uuid();