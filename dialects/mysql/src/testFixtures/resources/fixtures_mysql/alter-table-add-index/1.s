CREATE TABLE unique_data(
  data TEXT
);

ALTER TABLE unique_data
  ADD UNIQUE KEY key_data (data);

CREATE TABLE non_unique_data(
  data TEXT
);

ALTER TABLE unique_data
  ADD INDEX key_data (data);
