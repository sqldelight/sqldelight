package com.sample

import com.squareup.Redacted
import com.squareup.sqldelight.ColumnAdapter
import java.util.List
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Long
import kotlin.String

public data class Person(
  public val _id: Long,
  public val name: String,
  public val last_name: String?,
  public val is_cool: Boolean,
  public val friends: List<Person>?,
  public val shhh_its_secret: @Redacted String
) {
  public override fun toString(): String = """
  |Person [
  |  _id: $_id
  |  name: $name
  |  last_name: $last_name
  |  is_cool: $is_cool
  |  friends: $friends
  |  shhh_its_secret: $shhh_its_secret
  |]
  """.trimMargin()

  public class Adapter(
    public val friendsAdapter: ColumnAdapter<List<Person>, ByteArray>
  )
}
