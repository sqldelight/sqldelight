package com.sample

import com.squareup.Redacted
import com.squareup.sqldelight.ColumnAdapter
import java.util.List
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Long
import kotlin.String

data class Person(
  val _id: Long,
  val name: String,
  val last_name: String?,
  val is_cool: Boolean,
  val friends: List<Person>?,
  val shhh_its_secret: @Redacted String
) {
  override fun toString(): String = """
  |Person [
  |  _id: $_id
  |  name: $name
  |  last_name: $last_name
  |  is_cool: $is_cool
  |  friends: $friends
  |  shhh_its_secret: $shhh_its_secret
  |]
  """.trimMargin()

  class Adapter(
    val friendsAdapter: ColumnAdapter<List<Person>, ByteArray>
  )
}
