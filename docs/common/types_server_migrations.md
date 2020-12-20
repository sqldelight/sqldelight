## Custom Types in Migrations

If migrations are the schema's source of truth, you can also specify
the exposed kotlin type when altering a table:

```sql
import kotlin.String;
import kotlin.collection.List;

ALTER TABLE my_table
  ADD COLUMN new_column VARCHAR(8) AS List<String>;
```