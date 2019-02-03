package com.example

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

interface Player {
    val name: String

    val number: Long

    val team: String?

    val shoots: Shoots

    class Adapter(internal val shootsAdapter: ColumnAdapter<Shoots, String>)

    data class Impl(
        override val name: String,
        override val number: Long,
        override val team: String?,
        override val shoots: Shoots
    ) : Player {
        override fun toString(): String = buildString {
            appendln("Player.Impl [")
            appendln("""  name: $name""")
            appendln("""  number: $number""")
            appendln("""  team: $team""")
            appendln("""  shoots: $shoots""")
            append("]")
        }
    }
}
