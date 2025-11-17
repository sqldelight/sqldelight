CREATE TABLE SomeTable (
  id INTEGER PRIMARY KEY,
  txt TEXT NOT NULL
);

CREATE VIEW VSomeTable AS
SELECT * FROM SomeTable;

COMMENT ON TABLE SomeTable IS 'Stores text for every row';
COMMENT ON VIEW VSomeTable IS 'A View for every row';
COMMENT ON COLUMN SomeTable.id IS 'Integer Primary Key column';
COMMENT ON TABLE SomeTable IS NULL;

--error[col 17]: No table found with name Xyz
COMMENT ON TABLE Xyz IS NULL;
--error[col 16]:  No table found with name VXyz
COMMENT ON VIEW VXyz IS NULL;
--error[col 28]: No column found with name x
COMMENT ON COLUMN SomeTable.x IS NULL;
