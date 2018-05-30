package com.sample

import com.squareup.Redacted
import java.util.List
import kotlin.Double
import kotlin.String

interface PersonAndFriends {
    val full_name: String

    val friends: List<Person>?

    val shhh_its_secret: @Redacted String

    val casted: Double

    data class Impl(
            override val full_name: String,
            override val friends: List<Person>?,
            override val shhh_its_secret: @Redacted String,
            override val casted: Double
    ) : PersonAndFriends
}
