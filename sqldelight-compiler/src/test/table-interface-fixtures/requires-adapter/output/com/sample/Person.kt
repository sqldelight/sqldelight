package com.sample

import app.cash.sqldelight.ColumnAdapter
import com.squareup.Redacted
import java.util.List
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Long
import kotlin.String

/**
 * This is a person
 */
public data class Person(
  public val _id: Long,
  public val name: String,
  public val last_name: String?,
  public val is_cool: Boolean,
  public val friends: List<Person>?,
  @Redacted
  public val shhh_its_secret: String,
) {
  public class Adapter(
    public val friendsAdapter: ColumnAdapter<List<Person>, ByteArray>,
    public val shhh_its_secretAdapter: ColumnAdapter<String, String>,
  )
}
