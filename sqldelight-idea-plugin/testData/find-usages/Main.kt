@file:Suppress("ktlint:standard:filename")

package com.example

fun main() {
  val exampleQueries = ExampleQueries()
  val query = exampleQueries.selectById(id = 1L)
  val id = query.executeAsOneOrNull()?.id
}
