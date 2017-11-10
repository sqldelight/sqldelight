package com.sample

import kotlin.Boolean
import kotlin.String

interface JoinedWithUsing {
  val name: String

  val is_cool: Boolean

  data class Impl(override val name: String, override val is_cool: Boolean) : JoinedWithUsing
}

abstract class JoinedWithUsingModel : JoinedWithUsing {
  final override val name: String
    get() = name()

  final override val is_cool: Boolean
    get() = is_cool()

  abstract fun name(): String

  abstract fun is_cool(): Boolean
}
