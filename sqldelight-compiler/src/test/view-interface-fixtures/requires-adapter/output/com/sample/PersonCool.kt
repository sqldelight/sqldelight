package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class PersonCool(
  public val _id: Long,
  public val name: String,
  public val last_name: String?,
  public val is_cool: Boolean,
  public val friends: List<Person>?,
  @Redacted
  public val shhh_its_secret: String,
  public val how_cool: String
)
