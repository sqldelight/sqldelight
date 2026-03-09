CREATE TABLE Documents(
  context TEXT NOT NULL
);

ALTER TABLE Documents SET (fillfactor = 70);

ALTER TABLE Documents SET (
  autovacuum_vacuum_scale_factor     = 0.01,
  autovacuum_analyze_scale_factor    = 0.005,
  autovacuum_vacuum_cost_delay       = 2
);

ALTER TABLE Documents SET (toast_tuple_target = 4096);

ALTER TABLE Documents SET (parallel_workers = 4);

ALTER TABLE Documents RESET (autovacuum_vacuum_scale_factor);
