package com.squareup.sqlite.android.model

import com.squareup.sqlite.android.SqlitePluginException
import com.squareup.sqlite.android.model.ColumnConstraint.NotNullConstraint
import com.google.common.base.CaseFormat
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import java.util.ArrayList

import com.squareup.javapoet.ClassName.bestGuess

class Column<T>(internal val name: String, val type: Type, fullyQualifiedClass: String? = null,
    originatingElement: T) : SqlElement<T>(originatingElement) {
  enum class Type internal constructor(internal val defaultType: TypeName?, val replacement: String) {
    INT(TypeName.INT, "INTEGER"),
    LONG(TypeName.LONG, "INTEGER"),
    SHORT(TypeName.SHORT, "INTEGER"),
    DOUBLE(TypeName.DOUBLE, "REAL"),
    FLOAT(TypeName.FLOAT, "REAL"),
    BOOLEAN(TypeName.BOOLEAN, "INTEGER"),
    STRING(ClassName.get(String::class.java), "TEXT"),
    BLOB(ArrayTypeName.of(TypeName.BYTE), "BLOB"),
    ENUM(null, "TEXT"),
    CLASS(null, "BLOB")
  }

  private val classType: TypeName?

  internal val javaType: TypeName
    get() = when {
      type.defaultType == null && classType != null -> classType
      type.defaultType == null -> throw SqlitePluginException(originatingElement as Any,
          "Couldnt make a guess for type of colum " + name)
      notNullConstraint != null -> type.defaultType
      else -> type.defaultType.box()
    }

  val constraints: MutableList<ColumnConstraint<T>> = ArrayList()
  val isHandledType: Boolean
    get() = type != Type.CLASS
  val isNullable: Boolean
    get() = notNullConstraint == null
  val fieldName: String
    get() = fieldName(name)
  val methodName: String
    get() = methodName(name)
  val notNullConstraint: NotNullConstraint<T>?
    get() = constraints.filterIsInstance<NotNullConstraint<T>>().firstOrNull()

  init {
    var className = fullyQualifiedClass
    try {
      classType = when {
        className == null -> null
        className.startsWith("\'") -> bestGuess(className.substring(1, className.length - 1))
        else -> bestGuess(className)
      }
    } catch (ignored: IllegalArgumentException) {
      classType = null
    }
  }

  companion object {
    fun fieldName(name: String) = name.toUpperCase()
    fun methodName(name: String) =  CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name)
  }
}
