package app.cash.sqldelight.core.test

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec

/**
 * Added to make sure imports are resolved, since we're referencing constructors in generated code and Kotlin doesn't
 * support that when specifying the full package name inline (e.g. `::MyClass` works but `com.example::MyClass`
 * doesn't) - which we're doing when calling [FunSpec.toString]. This should only be necessary when calling
 * [SelectQueryGenerator.defaultResultTypeFunction], not for [SelectQueryGenerator.customResultTypeFunction].
 */
internal fun FunSpec.fileContents(): String = FileSpec
  .builder("com.example", "Test")
  .addFunction(this)
  .build()
  .toString()
