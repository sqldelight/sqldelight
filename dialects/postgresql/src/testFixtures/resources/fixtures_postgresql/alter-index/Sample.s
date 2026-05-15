CREATE TABLE abg (
   id INTEGER PRIMARY KEY,
   alpha TEXT,
   beta TEXT,
   gamma TEXT
);

CREATE INDEX CONCURRENTLY beta_gamma_idx ON abg (beta, gamma);

CREATE INDEX gamma_index_name ON abg (gamma) WHERE beta = 'some_value';

CREATE INDEX alpha_index_name ON abg USING BTREE (alpha) WITH (fillfactor = 70, deduplicate_items = on);

ALTER INDEX IF EXISTS beta_gamma_idx RENAME TO beta_gamma_idx_renamed;

ALTER INDEX gamma_index_name SET (buffering = on);

ALTER INDEX IF EXISTS gamma_index_name SET (fillfactor = 70, deduplicate_items = on);

ALTER INDEX IF EXISTS gamma_index_name SET (deduplicate_items = off);

ALTER INDEX alpha_index_name RESET (fillfactor);

ALTER INDEX alpha_index_name RESET (buffering, deduplicate_items);

ALTER INDEX alpha_index_name RESET (something_unknown);

REINDEX TABLE abg;

REINDEX INDEX CONCURRENTLY beta_gamma_idx_renamed;
