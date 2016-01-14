package com.squareup.sqlite.android.model

import com.squareup.sqlite.android.SqliteCompiler
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import java.util.ArrayList

class Table<T>(val packageName: String, internal val name: String, val sqlTableName: String,
    originatingElement: T, val isKeyValue: Boolean) : SqlElement<T>(originatingElement) {
  val columns = ArrayList<Column<T>>()
  val interfaceName: String
    get() = SqliteCompiler.interfaceName(name)
  val interfaceType: TypeName
    get() = ClassName.get(packageName, interfaceName)
}
