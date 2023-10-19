CREATE TABLE abg (
    id INTEGER PRIMARY KEY,
    alpha TEXT,
    beta TEXT,
    gamma TEXT
);

CREATE INDEX CONCURRENTLY beta_gamma_idx ON abg (beta, gamma);
