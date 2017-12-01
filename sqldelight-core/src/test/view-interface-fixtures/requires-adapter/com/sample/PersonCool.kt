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
    ) : PersonCool
}

abstract class PersonCoolModel : PersonCool {
    final override val _id: Long
        get() = _id()

    final override val name: String
        get() = name()

    final override val last_name: String?
        get() = last_name()

    final override val is_cool: Boolean
        get() = is_cool()

    final override val friends: List<Person>?
        get() = friends()

    final override val shhh_its_secret: @Redacted String
        get() = shhh_its_secret()

    final override val how_cool: String
        get() = how_cool()

    abstract fun _id(): Long

    abstract fun name(): String

    abstract fun last_name(): String?

    abstract fun is_cool(): Boolean

    abstract fun friends(): List<Person>?

    abstract fun shhh_its_secret(): @Redacted String

    abstract fun how_cool(): String
}
