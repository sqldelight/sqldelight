{% include 'common/transactions.md' %}
## Consistency of Large Queries

Using `AndroidSqliteDriver`, query results are read through a `CursorWindow` of limited size (2 MB by default). When
a result does not fit in one window, `AndroidSqliteDriver` executes the statement again to fill the next one. A
query executed outside a transaction can observe changes committed by another thread between windows,
and return inconsistent rows from the database.

To guarantee that a large query reads a single consistent snapshot, execute it inside a transaction.

```kotlin
val players: List<Player> = database.playerQueries.transactionWithResult {
  database.playerQueries.selectAll().executeAsList()
}
```

Using a transaction means other writers wait until the transaction ends.

Setting a larger `windowSizeBytes` on `AndroidSqliteDriver` (API 28+) can avoid creating an additional window.

!!! info
    Avoid `INSERT`, `UPDATE` or `DELETE` statements with a `RETURNING` clause whose result may not
    fit in one window. The statement is executed again for each additional window, so its changes
    are applied more than once. A transaction does not prevent this.
