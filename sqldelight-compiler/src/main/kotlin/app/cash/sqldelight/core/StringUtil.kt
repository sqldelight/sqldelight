package app.cash.sqldelight.core

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecaseChar() else it }

fun String.decapitalize() = replaceFirstChar { if (it.isLowerCase()) it else it.lowercaseChar() }
