package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Boolean
import kotlin.Long
import kotlin.String

interface PersonCool {
    val _id: Long

    val name: String

    val last_name: String?

    val is_cool: Boolean

    val friends: List<Person>?

    val shhh_its_secret: @Redacted String

    val how_cool: String

    data class Impl(
        override val _id: Long,
        override val name: String,
        override val last_name: String?,
        override val is_cool: Boolean,
        override val friends: List<Person>?,
        override val shhh_its_secret: @Redacted String,
        override val how_cool: String
    ) : PersonCool {
        override fun toString(): String = buildString {
            appendln("PersonCool.Impl [")
            appendln("""  _id: $_id""")
            appendln("""  name: $name""")
            appendln("""  last_name: $last_name""")
            appendln("""  is_cool: $is_cool""")
            appendln("""  friends: $friends""")
            appendln("""  shhh_its_secret: $shhh_its_secret""")
            appendln("""  how_cool: $how_cool""")
            append("]")
        }
    }
}
