package com.sample

import kotlin.Boolean
import kotlin.String

interface JoinedWithUsing {
    val name: String

    val is_cool: Boolean

    data class Impl(override val name: String, override val is_cool: Boolean) : JoinedWithUsing {
        override fun toString(): String = buildString {
            appendln("JoinedWithUsing.Impl [")
            appendln("""  name: $name""")
            appendln("""  is_cool: $is_cool""")
            append("]")
        }
    }
}
