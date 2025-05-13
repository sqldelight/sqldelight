CREATE TABLE abg (
    id INTEGER PRIMARY KEY,
    alpha TEXT,
    beta TEXT,
    gamma TEXT
);

CREATE INDEX CONCURRENTLY beta_gamma_idx ON abg (beta, gamma);

CREATE INDEX gamma_index_name ON abg (gamma) WHERE beta = 'some_value';

CREATE INDEX alpha_index_name ON abg USING BTREE (alpha) WITH (fillfactor = 70, deduplicate_items = on);

CREATE INDEX beta_gamma_index_name ON abg USING HASH (beta) WITH (fillfactor = 20);
-- error[col 87]: invalid value for boolean option "deduplicate_items" yes
CREATE INDEX alpha_index_name_err ON abg USING BTREE (alpha) WITH (deduplicate_items = yes);
-- error[col 83]: value 1 out of bounds for option "fillfactor"
CREATE INDEX beta_gamma_index_name_err ON abg USING HASH (beta) WITH (fillfactor = 1);
-- error[col 76]:  unrecognized parameter "autosummarize"
CREATE INDEX beta_gamma_index_name_err_param ON abg USING HASH (beta) WITH (autosummarize = off);

CREATE TABLE json_gin(
  alpha JSONB,
  beta JSONB
);

CREATE TABLE json_gist(
  alpha JSONB,
  beta JSONB
);

CREATE TABLE text_search(
  alpha TSVECTOR,
  beta TEXT
);

CREATE INDEX gin_alpha_1 ON json_gin USING GIN (alpha);
CREATE INDEX gin_alpha_beta_2 ON json_gin USING GIN (alpha, beta);
CREATE INDEX gin_alpha_beta_3 ON json_gin USING GIN (alpha jsonb_ops, beta);
CREATE INDEX gin_alpha_beta_4 ON json_gin USING GIN (alpha, beta jsonb_path_ops) WITH (fastupdate = off);
CREATE INDEX gin_alpha_beta_5 ON json_gin USING GIN (alpha jsonb_path_ops, beta jsonb_ops) WITH (gin_pending_list_limit = 2048);

CREATE INDEX gist_alpha_1 ON text_search USING GIST (alpha) WITH (fillfactor = 75);
CREATE INDEX gist_alpha_2 ON text_search USING GIST (alpha) WITH (buffering = on);

CREATE INDEX tsv_gist_alpha_1 ON text_search USING GIST (alpha);
CREATE INDEX tsv_gin_alpha_1 ON text_search USING GIN (alpha);
CREATE INDEX trgm_gist_beta_1 ON text_search USING GIST (beta gist_trgm_ops(siglen=32));
CREATE INDEX trgm_gist_beta_2 ON text_search USING GIN (beta gin_trgm_ops);

CREATE INDEX beta_index ON text_search (beta varchar_pattern_ops);

CREATE INDEX ts_brin_beta_1 ON text_search USING BRIN (beta) WITH (autosummarize = on, pages_per_range = 6);

-- error[col 128]: value 1 out of bounds for option "gin_pending_list_limit"
CREATE INDEX gin_alpha_beta_error_1 ON json_gin USING GIN (alpha jsonb_path_ops, beta jsonb_ops) WITH (gin_pending_list_limit = 1);
-- error[col 106]: invalid value for boolean option "fastupdate" yes
CREATE INDEX gin_alpha_beta_error_2 ON json_gin USING GIN (alpha, beta jsonb_path_ops) WITH (fastupdate = yes);
-- error[col 91]:  value 0 out of bounds for option "pages_per_range"
CREATE INDEX ts_brin_beta_error_1 ON text_search USING BRIN (beta) WITH (pages_per_range = 0);
-- error[col 87]: invalid value for boolean option "autosummarize" no
CREATE INDEX ts_brin_beta_error_2 ON text_search USING BRIN (beta) WITH (autosummarize=no);
