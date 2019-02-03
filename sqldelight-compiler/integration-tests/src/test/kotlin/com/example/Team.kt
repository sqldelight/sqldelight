package com.example

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.core.integration.Shoots
import kotlin.Long
import kotlin.String

interface Team {
    val name: String

    val captain: Long

    val inner_type: Shoots.Type?

    val coach: String

    class Adapter(internal val inner_typeAdapter: ColumnAdapter<Shoots.Type, String>)

    data class Impl(
        override val name: String,
        override val captain: Long,
        override val inner_type: Shoots.Type?,
        override val coach: String
    ) : Team {
        override fun toString(): String = buildString {
            appendln("Team.Impl [")
            appendln("""  name: $name""")
            appendln("""  captain: $captain""")
            appendln("""  inner_type: $inner_type""")
            appendln("""  coach: $coach""")
            append("]")
        }
    }
}
