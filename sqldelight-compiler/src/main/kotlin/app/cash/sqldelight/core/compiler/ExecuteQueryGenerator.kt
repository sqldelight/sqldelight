package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.compiler.model.NamedExecute
import app.cash.sqldelight.core.lang.ASYNC_RESULT_TYPE
import app.cash.sqldelight.core.lang.QUERY_RESULT_TYPE
import app.cash.sqldelight.core.lang.argumentType
import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
      .apply { if (mutatorReturns) addCode("return result") }
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
      .apply {
        val type = when {
          generateAsync && query.statement is SqlDelightStmtClojureStmtList -> ASYNC_RESULT_TYPE.parameterizedBy(LONG)
          generateAsync -> LONG
          else -> QUERY_RESULT_TYPE.parameterizedBy(LONG)
        }
        returns(type, CodeBlock.of("The number of rows updated."))
      }
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), KModifier.PRIVATE)
      .initializer("${query.name.capitalize()}()")
      .build()
  }
}
