package com.example

import kotlin.Long
import kotlin.String

interface Team {
    val name: String

    val captain: Long

    val coach: String

    data class Impl(
            override val name: String,
            override val captain: Long,
            override val coach: String
    ) : Team
}

abstract class TeamModel : Team {
    final override val name: String
        get() = name()

    final override val captain: Long
        get() = captain()

    final override val coach: String
        get() = coach()

    abstract fun name(): String

    abstract fun captain(): Long

    abstract fun coach(): String
}
