CREATE TABLE index_data(
  data TEXT,
  INDEX idx_data (data)
);

ALTER TABLE index_data
  DROP KEY idx_data;

CREATE TABLE key_data(
  data TEXT,
  KEY idx_data (data)
);

ALTER TABLE key_data
  DROP KEY key_data;
