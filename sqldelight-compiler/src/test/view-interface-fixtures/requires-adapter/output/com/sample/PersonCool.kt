package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Boolean
import kotlin.Long
import kotlin.String

data class PersonCool(
  val _id: Long,
  val name: String,
  val last_name: String?,
  val is_cool: Boolean,
  val friends: List<Person>?,
  val shhh_its_secret: @Redacted String,
  val how_cool: String
) {
  override fun toString(): String = """
  |PersonCool [
  |  _id: $_id
  |  name: $name
  |  last_name: $last_name
  |  is_cool: $is_cool
  |  friends: $friends
  |  shhh_its_secret: $shhh_its_secret
  |  how_cool: $how_cool
  |]
  """.trimMargin()
}
