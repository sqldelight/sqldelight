CREATE TABLE stuff (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  use_after smallint NOT NULL,
  updated_by varchar(255) NOT NULL,
  created_at timestamp(3) NOT NULL DEFAULT NOW(3),
  updated_at timestamp(3) NOT NULL DEFAULT NOW(3) ON UPDATE NOW(3),
  other_at timestamp(3) NOT NULL DEFAULT NOW() ON UPDATE NOW(),
  KEY idx_credentials_updated_at(updated_at),
  UNIQUE (use_after)
);
