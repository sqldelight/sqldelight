package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.model.NamedExecute
import app.cash.sqldelight.core.lang.argumentType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

open class ExecuteQueryGenerator(
  private val query: NamedExecute,
) : QueryGenerator(query) {

  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    return interfaceFunction()
      .addCode(executeBlock())
      .build()
  }

  fun interfaceFunction(): FunSpec.Builder {
    return FunSpec.builder(query.name)
      .apply { if (generateAsync) addModifiers(SUSPEND) }
      .also(this::addJavadoc)
      .addParameters(
        query.parameters.map {
          ParameterSpec.builder(it.name, it.argumentType()).build()
        },
      )
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), KModifier.PRIVATE)
      .initializer("${query.name.capitalize()}()")
      .build()
  }
}
