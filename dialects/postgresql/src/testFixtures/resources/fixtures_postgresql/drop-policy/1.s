CREATE TABLE org (
  id INTEGER
);

CREATE POLICY org_org_id
  ON org
  USING (id > 0);

DROP POLICY org_org_id ON org;
DROP POLICY IF EXISTS org_org_id ON org CASCADE;
