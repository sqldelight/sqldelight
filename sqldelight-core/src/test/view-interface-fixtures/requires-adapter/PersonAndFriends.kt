package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Float
import kotlin.String

interface PersonAndFriends {
  fun full_name(): String

  fun friends(): List<Person>?

  fun shhh_its_secret(): @Redacted String

  fun casted(): Float

  data class Impl(override val full_name: String, override val friends: List<Person>?,
      override val shhh_its_secret: @Redacted String,
      override val casted: Float) : PersonAndFriendsKt
}

interface PersonAndFriendsKt : PersonAndFriends {
  val full_name: String

  val friends: List<Person>?

  val shhh_its_secret: @Redacted String

  val casted: Float

  override fun full_name(): String = full_name

  override fun friends(): List<Person>? = friends

  override fun shhh_its_secret(): @Redacted String = shhh_its_secret

  override fun casted(): Float = casted
}
