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

    abstract fun name(): String
}
