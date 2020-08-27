## API

If you want to execute multiple statements under one transaction, use `transaction` function. 

```kotlin
val players = listOf<Player>()
database.playerQueries.transaction {
  players.forEach { player ->
    database.playerQueries.insert(
      player_number = player.number,
      full_name = player.fullName
    )
  }
}
```

To return a value from a transaction, use the `transactionWithResult` function.

```kotlin
val players: List<Player> = database.playerQueries.transactionWithResult {
  database.playerQueries.selectAll().executeAsList()
}
```

## Rollback

Transactions will roll back if an exception occurs anywhere in them. You can manually roll back a
transaction anywhere inside of one, but if your transaction returns a value you will need to specify
a value for the transaction to return.

```kotlin
database.playerQueries.transaction {
  players.forEach { player ->
    if (player.number == 0) rollback()
    database.playerQueries.insert(
      player_number = player.number,
      full_name = player.fullName
    )
  }
}
```

```kotlin
val numberInserted: Int = database.playerQueries.transactionWithResult {
  players.forEach { player ->
    if (player.number == 0) rollback(0)
    database.playerQueries.insert(
      player_number = player.number,
      full_name = player.fullName
    )
  }
  players.size
}
```

## Callbacks

You can register callbacks to occur after a transaction has completed or rolled back:

```kotlin
database.playerQueries.transaction {
  afterRollback { log("No players were inserted.") }
  afterCommit { log("${players.size} players were inserted.") }

  players.forEach { player ->
    database.playerQueries.insert(
      player_number = player.number,
      full_name = player.fullName
    )
  }
}
```