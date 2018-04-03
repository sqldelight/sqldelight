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
    ) : Team
}

abstract class TeamModel : Team {
    final override val name: String
        get() = name()

    final override val captain: Long
        get() = captain()

    final override val inner_type: Shoots.Type?
        get() = inner_type()

    final override val coach: String
        get() = coach()

    abstract fun name(): String

    abstract fun captain(): Long

    abstract fun inner_type(): Shoots.Type?

    abstract fun coach(): String
}
