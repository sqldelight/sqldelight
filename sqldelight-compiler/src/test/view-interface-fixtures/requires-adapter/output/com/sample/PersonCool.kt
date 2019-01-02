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
        override fun toString(): String = """
        |PersonCool.Impl [
        |  _id: $_id
        |  name: $name
        |  last_name: $last_name
        |  is_cool: $is_cool
        |  friends: $friends
        |  shhh_its_secret: $shhh_its_secret
        |  how_cool: $how_cool
        |]
        """.trimMargin()
    }
}
