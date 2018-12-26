package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

expect fun copyOnWriteList(): MutableList<Query<*>>