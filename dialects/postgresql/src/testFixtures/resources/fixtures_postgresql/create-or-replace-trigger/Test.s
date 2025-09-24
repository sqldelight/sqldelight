CREATE TABLE accounts(
  id INT GENERATED ALWAYS AS IDENTITY,
  balance REAL
);

CREATE TABLE accounts_audit (
   account_id INT,
   balance REAL,
   changed_on TIMESTAMP NOT NULL
);

CREATE OR REPLACE FUNCTION account_audit_update()
RETURNS TRIGGER LANGUAGE PLPGSQL AS
$$
BEGIN
    IF new.balance <> old.balance THEN
    INSERT INTO accounts_audit(account_id, balance, changed_on) VALUES (old.id, old.balance, NOW());
    END IF;
    RETURN new;
END;
$$;

CREATE OR REPLACE TRIGGER check_update
    BEFORE UPDATE OF balance ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION account_audit_update();

CREATE TABLE organizations (
  id INTEGER NOT NULL,
  name TEXT NOT NULL,
  updated_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE OR REPLACE FUNCTION organizations_set_updated_at()
RETURNS TRIGGER LANGUAGE PLPGSQL AS
$$
BEGIN
  new.updated_at := NOW();
  RETURN new;
END;
$$;

CREATE TRIGGER organizations_set_updated_at
BEFORE UPDATE ON organizations
FOR EACH ROW
EXECUTE FUNCTION organizations_set_updated_at();

CREATE TABLE user_profile (
  username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL,
  password_strength TEXT
);

CREATE OR REPLACE FUNCTION user_profile_password_strength()
RETURNS TRIGGER LANGUAGE PLPGSQL AS $$
BEGIN

  IF new.password = old.password THEN
    RETURN new;
  END IF;

  IF length(new.password) < 6 THEN
    new.password_strength := 'weak';
  ELSIF length(new.password) < 12 THEN
    new.password_strength := 'medium';
  ELSE
    new.password_strength := 'strong';
  END IF;

  RETURN new;
END;
$$;

CREATE TRIGGER user_profile_password_strength
BEFORE INSERT OR UPDATE OF password
ON user_profile
FOR EACH ROW
EXECUTE FUNCTION user_profile_password_strength();
