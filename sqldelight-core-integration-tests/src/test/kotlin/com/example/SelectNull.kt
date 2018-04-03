package com.example

import java.lang.Void

interface SelectNull {
    val expr: Void?

    data class Impl(override val expr: Void?) : SelectNull
}

abstract class SelectNullModel : SelectNull {
    final override val expr: Void?
        get() = expr()

    abstract fun expr(): Void?
}
