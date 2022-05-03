package app.cash.sqldelight.core.compiler

import app.cash.sqldelight.core.compiler.model.NamedExecute
import app.cash.sqldelight.core.compiler.model.NamedMutator
import app.cash.sqldelight.core.lang.ASYNC_DRIVER_CALLBACK_TYPE
import app.cash.sqldelight.core.lang.argumentType
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.TableNameElement
import app.cash.sqldelight.core.psi.SqlDelightStmtClojureStmtList
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName

open class ExecuteQueryGenerator(
  private val query: NamedExecute,
  private val generateAsync: Boolean = false
) : QueryGenerator(query, generateAsync) {
  internal open fun tablesUpdated(): List<TableNameElement> {
    if (query.statement is SqlDelightStmtClojureStmtList) {
      return PsiTreeUtil.findChildrenOfAnyType(
        query.statement,
        SqlUpdateStmtLimited::class.java,
        SqlDeleteStmtLimited::class.java,
        SqlInsertStmt::class.java
      ).flatMap {
        MutatorQueryGenerator(
          when (it) {
            is SqlUpdateStmtLimited -> NamedMutator.Update(it, query.identifier as StmtIdentifierMixin)
            is SqlDeleteStmtLimited -> NamedMutator.Delete(it, query.identifier as StmtIdentifierMixin)
            is SqlInsertStmt -> NamedMutator.Insert(it, query.identifier as StmtIdentifierMixin)
            else -> throw IllegalArgumentException("Unexpected statement $it")
          },
          generateAsync
        ).tablesUpdated()
      }.distinctBy { it.name }
    }
    return emptyList()
  }

  private fun FunSpec.Builder.notifyQueries(): FunSpec.Builder {
    val tablesUpdated = tablesUpdated()

    if (tablesUpdated.isEmpty()) return this

    // The list of affected tables:
    // notifyQueries { emit ->
    //     emit("players")
    //     emit("teams")
    // }
    addCode(
      CodeBlock.builder()
        .apply { if (generateAsync) beginControlFlow(".onSuccess") }
        .beginControlFlow("notifyQueries(%L) { emit ->", query.id)
        .apply {
          tablesUpdated.sortedBy { it.name }.forEach {
            addStatement("emit(\"${it.name}\")")
          }
        }
        .endControlFlow()
        .apply {
          if (generateAsync) {
            endControlFlow()
            add(".%M {}", MemberName("app.cash.sqldelight.async.db", "map"))
          }
        }
        .build()
    )

    return this
  }

  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    return interfaceFunction()
      .apply {
        if (generateAsync) {
          returns(ASYNC_DRIVER_CALLBACK_TYPE.parameterizedBy(Unit::class.asTypeName()))
        }
      }
      .addCode(executeBlock())
      .notifyQueries()
      .build()
  }

  fun interfaceFunction(): FunSpec.Builder {
    return FunSpec.builder(query.name)
      .also(this::addJavadoc)
      .addParameters(
        query.parameters.map {
          ParameterSpec.builder(it.name, it.argumentType()).build()
        }
      )
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), KModifier.PRIVATE)
      .initializer("${query.name.capitalize()}()")
      .build()
  }
}
