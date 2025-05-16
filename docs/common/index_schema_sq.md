{% if not server %}## Defining the Schema{% endif %}

Write your SQL statements in a `.sq` file under `src/main/sqldelight`. 
Typically the first statement in the `.sq` file creates a table, but you can also create indexes
or set up default content.

```sql title="src/main/sqldelight/com/example/sqldelight/hockey/data/Player.sq"
CREATE TABLE hockeyPlayer (
  player_number INTEGER PRIMARY KEY NOT NULL,
  full_name TEXT NOT NULL
);

CREATE INDEX hockeyPlayer_full_name ON hockeyPlayer(full_name);

INSERT INTO hockeyPlayer (player_number, full_name)
VALUES (15, 'Ryan Getzlaf');
```
