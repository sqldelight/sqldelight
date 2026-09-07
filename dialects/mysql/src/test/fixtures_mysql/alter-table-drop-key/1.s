CREATE TABLE parent (
    id INT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE child (
    id INT,
    parent_id INT,
    FOREIGN KEY (parent_id)  REFERENCES parent(id)
);

ALTER TABLE parent
  DROP PRIMARY KEY;

ALTER TABLE child
  DROP FOREIGN KEY parent_id;

-- Should fail, id isn't a foreign key
ALTER TABLE child
  DROP FOREIGN KEY id;
