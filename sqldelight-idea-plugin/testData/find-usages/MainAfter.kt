@file:Suppress("ktlint:standard:filename")

package com.example

fun main() {
  val exampleQueries = ExampleQueries()
  val query = exampleQueries.selectById(newId = 1L)
  val id = query.executeAsOneOrNull()?.newId
}
