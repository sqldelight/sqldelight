CREATE TABLE test (
  external_event_id TEXT
);

ALTER TABLE test
  ADD CONSTRAINT `idx_external_event_id`
  UNIQUE (`external_event_id`);