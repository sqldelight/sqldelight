## Optimistic Locking

If you specify a column as a `LOCK`, it would have a value type generated for it, and also require
that `UPDATE` statements correctly use the lock to perform updates.

```sql
CREATE TABLE hockeyPlayer(
  id INT AS VALUE,
  version_number INT AS LOCK,
  name VARCHAR(8)
);

-- This will fail (and the IDE plugin will suggest rewriting to the below)
updateName:
UPDATE hockeyPlayer
SET name = ?;

-- This will pass compilation
updateNamePassing:
UPDATE hockeyPlayer
SET name = ?
    version_number = :version_number + 1
WHERE version_number = :version_number;
```

## Custom Types in Migrations

If migrations are the schema's source of truth, you can also specify
the exposed kotlin type when altering a table:

```sql
import kotlin.String;
import kotlin.collection.List;

ALTER TABLE my_table
  ADD COLUMN new_column VARCHAR(8) AS List<String>;
```
