package com.sample

import kotlin.Boolean
import kotlin.String

interface JoinedWithUsing {
  fun name(): String

  fun is_cool(): Boolean

  data class Impl(override val name: String, override val is_cool: Boolean) : JoinedWithUsingKt
}

interface JoinedWithUsingKt : JoinedWithUsing {
  val name: String

  val is_cool: Boolean

  override fun name(): String = name

  override fun is_cool(): Boolean = is_cool
}
