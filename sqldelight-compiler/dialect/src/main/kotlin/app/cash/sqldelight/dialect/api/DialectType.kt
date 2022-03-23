package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

interface DialectType {

  val javaType: TypeName

  fun decode(value: CodeBlock): CodeBlock = value

  fun encode(value: CodeBlock): CodeBlock = value

  fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock

  fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock
}

