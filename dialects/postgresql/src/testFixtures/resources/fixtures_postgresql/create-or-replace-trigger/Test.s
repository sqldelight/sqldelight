CREATE TABLE accounts(
  id INT GENERATED ALWAYS AS IDENTITY,
  balance REAL
);

CREATE TABLE accounts_audit (
   account_id INT,
   balance REAL,
   changed_on TIMESTAMP NOT NULL
);

CREATE OR REPLACE TRIGGER check_update
    BEFORE UPDATE OF balance ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION account_audit_update();

CREATE OR REPLACE FUNCTION account_audit_update()
RETURNS TRIGGER LANGUAGE PLPGSQL AS
$$
BEGIN
    INSERT INTO accounts_audit(account_id, balance, changed_on) VALUES (old.id, old.balance, NOW());
    RETURN new;
END;
$$;

CREATE TABLE organizations (
  id INTEGER NOT NULL,
  name TEXT NOT NULL,
  updated_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE TRIGGER organizations_set_updated_at
BEFORE UPDATE ON organizations
FOR EACH ROW
EXECUTE FUNCTION organizations_set_updated_at();

CREATE OR REPLACE FUNCTION organizations_set_updated_at()
RETURNS TRIGGER LANGUAGE PLPGSQL AS
$$
BEGIN
  new.updated_at := NOW();
  RETURN new;
END;
$$;
