package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Boolean
import kotlin.Long
import kotlin.String

interface PersonCool {
  fun _id(): Long

  fun name(): String

  fun last_name(): String?

  fun is_cool(): Boolean

  fun friends(): List<Person>?

  fun shhh_its_secret(): @Redacted String

  fun how_cool(): String

  data class Impl(override val _id: Long, override val name: String,
      override val last_name: String?, override val is_cool: Boolean,
      override val friends: List<Person>?, override val shhh_its_secret: @Redacted String,
      override val how_cool: String) : PersonCoolKt
}

interface PersonCoolKt : PersonCool {
  val _id: Long

  val name: String

  val last_name: String?

  val is_cool: Boolean

  val friends: List<Person>?

  val shhh_its_secret: @Redacted String

  val how_cool: String

  override fun _id(): Long = _id

  override fun name(): String = name

  override fun last_name(): String? = last_name

  override fun is_cool(): Boolean = is_cool

  override fun friends(): List<Person>? = friends

  override fun shhh_its_secret(): @Redacted String = shhh_its_secret

  override fun how_cool(): String = how_cool
}
