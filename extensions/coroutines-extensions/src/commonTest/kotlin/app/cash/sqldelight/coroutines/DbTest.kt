package app.cash.sqldelight.coroutines

interface DbTest {
  suspend fun setupDb(): TestDb
}
