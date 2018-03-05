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
    ) : Player
}

abstract class PlayerModel : Player {
    final override val name: String
        get() = name()

    final override val number: Long
        get() = number()

    final override val team: String?
        get() = team()

    final override val shoots: Shoots
        get() = shoots()

    abstract fun name(): String

    abstract fun number(): Long

    abstract fun team(): String?

    abstract fun shoots(): Shoots
}
