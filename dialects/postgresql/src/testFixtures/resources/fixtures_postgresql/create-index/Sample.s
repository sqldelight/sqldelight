CREATE TABLE abg (
    id INTEGER PRIMARY KEY,
    alpha TEXT,
    beta TEXT,
    gamma TEXT
);

CREATE INDEX CONCURRENTLY beta_gamma_idx ON abg (beta, gamma);


CREATE TABLE json_gin(
  alpha JSONB,
  beta JSONB
);

CREATE INDEX gin_alpha_1 ON json_gin USING GIN (alpha);
CREATE INDEX gin_alpha_beta_2 ON json_gin USING GIN (alpha, beta);
CREATE INDEX gin_alpha_beta_3 ON json_gin USING GIN (alpha jsonb_ops, beta);
CREATE INDEX gin_alpha_beta_4 ON json_gin USING GIN (alpha, beta jsonb_path_ops);
CREATE INDEX gin_alpha_beta_5 ON json_gin USING GIN (alpha jsonb_path_ops, beta jsonb_ops);
