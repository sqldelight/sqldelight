CREATE TABLE parent2 (
    id INT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE child2 (
    id INT,
    parent_id2 INT REFERENCES parent2(id)
);

ALTER TABLE child2
  DROP FOREIGN KEY parent_id2;

-- Should fail, id isn't a foreign key
ALTER TABLE child2
  DROP FOREIGN KEY id;
