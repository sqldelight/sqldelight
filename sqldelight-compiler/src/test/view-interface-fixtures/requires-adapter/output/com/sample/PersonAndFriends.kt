package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Double
import kotlin.String

public data class PersonAndFriends(
  public val full_name: String,
  public val friends: List<Person>?,
  public val shhh_its_secret: @Redacted String,
  public val casted: Double
)
