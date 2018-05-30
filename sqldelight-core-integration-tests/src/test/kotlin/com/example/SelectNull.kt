package com.example

import java.lang.Void

interface SelectNull {
    val expr: Void?

    data class Impl(override val expr: Void?) : SelectNull
}
