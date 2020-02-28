# Transactions

If you want to execute multiple statements under one transaction, use `transaction` function. 

```kotlin
val players = listOf<Player>()
database.playerQueries.transaction {
  players.foreach { player ->
    database.playerQueries.insert(player_number = player.number, full_name = player.fullName)
  }
}
```

