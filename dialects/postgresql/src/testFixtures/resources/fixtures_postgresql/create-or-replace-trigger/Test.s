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

  IF TG_OP = 'UPDATE' AND new.password IS NOT DISTINCT FROM old.password THEN
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

CREATE TABLE emp (
    empname           TEXT NOT NULL,
    salary            INTEGER
);

CREATE TABLE emp_audit(
    operation         CHAR(1)   NOT NULL,
    stamp             TIMESTAMP NOT NULL,
    userid            TEXT      NOT NULL,
    empname           TEXT      NOT NULL,
    salary            INTEGER
);

CREATE OR REPLACE FUNCTION process_emp_audit()
RETURNS TRIGGER LANGUAGE PLPGSQL AS $$
BEGIN
    --
    -- Create a row in emp_audit to reflect the operation performed on emp,
    -- making use of the special variable TG_OP to work out the operation.
    --
    IF (TG_OP = 'DELETE') THEN
           INSERT INTO emp_audit SELECT 'D', NOW(), CURRENT_USER, old.empname, old.salary;
       ELSIF (TG_OP = 'UPDATE') THEN
           INSERT INTO emp_audit SELECT 'U', NOW(), CURRENT_USER, new.empname, new.salary;
       ELSIF (TG_OP = 'INSERT') THEN
           INSERT INTO emp_audit SELECT 'I', NOW(), CURRENT_USER, new.empname, new.salary;
       END IF;
    RETURN NULL; -- result is ignored since this is an AFTER trigger
END;
$$;

CREATE TRIGGER emp_audit
AFTER INSERT OR UPDATE OR DELETE ON emp
  FOR EACH ROW EXECUTE FUNCTION process_emp_audit();

CREATE OR REPLACE FUNCTION accounts_check_balance()
RETURNS TRIGGER LANGUAGE PLPGSQL AS $$
BEGIN
    RAISE NOTICE 'checking balance for account %', new.id;

    IF new.balance IS NULL THEN
        RAISE WARNING 'balance is null for account %', new.id;
    END IF;

    IF new.balance < old.balance THEN
        RAISE EXCEPTION 'balance decreased for account %', new.id USING ERRCODE = 'check_violation';
    END IF;

    IF new.balance < 0 THEN
        RAISE EXCEPTION 'negative balance not allowed' USING ERRCODE = '23514';
    END IF;

    RETURN new;
END;
$$;

CREATE OR REPLACE TRIGGER accounts_check_balance
    BEFORE UPDATE OF balance ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION accounts_check_balance();

CREATE OR REPLACE FUNCTION accounts_sync_audit()
RETURNS TRIGGER LANGUAGE PLPGSQL AS $$
BEGIN
    UPDATE accounts_audit SET balance = new.balance, changed_on = NOW() WHERE account_id = new.id;

    IF FOUND THEN
        RETURN new;
    END IF;

    INSERT INTO accounts_audit(account_id, balance, changed_on) VALUES (new.id, new.balance, NOW());

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    RETURN new;
END;
$$;

CREATE OR REPLACE TRIGGER accounts_sync_audit
    AFTER INSERT OR UPDATE OF balance ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION accounts_sync_audit();
