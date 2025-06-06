CREATE TABLE test_index_visibility (
  i INT,
  j INT,
  k INT,
  INDEX i_idx(i) INVISIBLE
);

ALTER TABLE test_index_visibility ADD INDEX j_idx(j) VISIBLE;
ALTER TABLE test_index_visibility ALTER INDEX i_idx INVISIBLE;
ALTER TABLE test_index_visibility ALTER INDEX i_idx VISIBLE, ALTER INDEX j_idx INVISIBLE;
