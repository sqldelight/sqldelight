package com.squareup.sqldelight.core.compiler

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * The maximum string length that we can emit as a literal. This value is 2^15 instead of the real
 * value, 2^16, to account for the indentation that KotlinPoet will add by using `trimMargin`.
 */
private const val MAX_STRING_LENGTH = 32_768

private val BUILD_STRING = MemberName("kotlin.text", "buildString")

/**
 * Returns a [CodeBlock] which contains a representation of [this] as a literal. If longer than
 * the maximum allowed length for a string, it will be broken up into multiple literals with code
 * to re-assemble at runtime.
 */
internal fun String.toCodeLiteral(): CodeBlock {
  if (length < MAX_STRING_LENGTH) {
    return CodeBlock.of("%S", this)
  }
  return buildCodeBlock {
    add("%M(%L) {\n", BUILD_STRING, length)
    for (i in 0 until length step MAX_STRING_LENGTH) {
      add("append(%S)\n", substring(i, (i + MAX_STRING_LENGTH).coerceAtMost(length)))
    }
    add("}")
  }
}
