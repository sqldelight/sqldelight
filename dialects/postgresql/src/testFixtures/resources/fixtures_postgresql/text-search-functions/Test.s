CREATE TABLE t1 (
  c1 TSVECTOR
);

INSERT INTO t1 (c1) VALUES ('the rain in spain falls mainly on the plains') ;

SELECT c1 @@ 'fail'
FROM t1;
