package com.squareup.sqldelight.runtime.coroutines

interface DbTest {
  suspend fun setupDb(): TestDb
}
