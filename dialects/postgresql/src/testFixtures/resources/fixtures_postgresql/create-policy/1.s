CREATE TABLE org (
  id INTEGER
);

CREATE POLICY org_org_id
  ON org
  USING (
    current_setting('app.allow_all_orgs', TRUE) = 'on'
    OR (id::text = current_setting('app.org_id', TRUE))
  );

SELECT current_setting('app.org_id', TRUE);
