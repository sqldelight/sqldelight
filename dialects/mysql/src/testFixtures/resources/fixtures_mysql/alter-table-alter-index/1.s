CREATE TABLE test_index_visibility (
  i INT,
  j INT,
  k INT,
  INDEX i_idx(i) invisible
);

ALTER TABLE test_index_visibility ADD INDEX j_idx(j) visible;
CREATE INDEX k_idx ON test_index_visibility(k) INVISIBLE;
ALTER TABLE test_index_visibility ALTER INDEX i_idx INVISIBLE;
ALTER TABLE test_index_visibility ALTER INDEX i_idx VISIBLE, ALTER INDEX j_idx INVISIBLE;
