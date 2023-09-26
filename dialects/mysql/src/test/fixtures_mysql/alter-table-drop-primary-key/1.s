CREATE TABLE primary_key_data(
  data TEXT,
  value INT,
  PRIMARY KEY (data)
);

ALTER TABLE primary_key_data
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (value);
