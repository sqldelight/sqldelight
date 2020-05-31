package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Double
import kotlin.String

data class PersonAndFriends(
  val full_name: String,
  val friends: List<Person>?,
  val shhh_its_secret: @Redacted String,
  val casted: Double
) {
  override fun toString(): String = """
  |PersonAndFriends [
  |  full_name: $full_name
  |  friends: $friends
  |  shhh_its_secret: $shhh_its_secret
  |  casted: $casted
  |]
  """.trimMargin()
}
