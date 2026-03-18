CREATE TABLE org (
  id INTEGER
);

CREATE POLICY org_org_id
  ON org
  USING (id > 0);

ALTER POLICY org_org_id ON org RENAME TO org_org_id_v2;
ALTER POLICY org_org_id_v2 ON org USING (id::text = '1');
ALTER POLICY org_org_id_v2 ON org WITH CHECK (id > 0);
ALTER POLICY org_org_id_v2 ON org TO PUBLIC;
