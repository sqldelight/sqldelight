package com.squareup.sqldelight.core.compiler

import com.alecstrong.sql.psi.core.psi.SqlDeleteStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.sqldelight.core.compiler.model.NamedExecute
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.psi.SqlDelightStmtClojureStmtList

open class ExecuteQueryGenerator(private val query: NamedExecute) : QueryGenerator(query) {
  internal open fun queriesUpdated(): List<NamedQuery> {
    if (query.statement is SqlDelightStmtClojureStmtList) {
      return PsiTreeUtil.findChildrenOfAnyType(
          query.statement,
          SqlUpdateStmtLimited::class.java,
          SqlDeleteStmtLimited::class.java,
          SqlInsertStmt::class.java
      ).flatMap {
        MutatorQueryGenerator(when (it) {
          is SqlUpdateStmtLimited -> NamedMutator.Update(it, query.identifier as StmtIdentifierMixin)
          is SqlDeleteStmtLimited -> NamedMutator.Delete(it, query.identifier as StmtIdentifierMixin)
          is SqlInsertStmt -> NamedMutator.Insert(it, query.identifier as StmtIdentifierMixin)
          else -> throw IllegalArgumentException("Unexpected statement $it")
        }).queriesUpdated()
      }.distinct()
    }
    return emptyList()
  }

  private fun FunSpec.Builder.notifyQueries(): FunSpec.Builder {
    val resultSetsUpdated = queriesUpdated()

    if (resultSetsUpdated.isEmpty()) return this

    // The list of affected queries:
    // notifyQueries { emit ->
    //     emit(queryWrapper.dataQueries.selectForId)
    //     emit(queryWrapper.otherQueries.selectForId)
    // }
    // TODO: Only notify queries that were dirtied (check using dirtied method).
    addCode(
      CodeBlock.builder()
        .beginControlFlow("notifyQueries { emit ->")
        .apply {
          resultSetsUpdated.forEach {
            addStatement("emit(%L)", it.queryProperty)
          }
        }
        .endControlFlow()
        .build()
    )
    return this
  }

  /**
   * The public api to execute [query]
   */
  fun function(): FunSpec {
    return interfaceFunction()
        .addModifiers(KModifier.OVERRIDE)
        .addCode(executeBlock())
        .notifyQueries()
        .build()
  }

  fun interfaceFunction(): FunSpec.Builder {
    return FunSpec.builder(query.name)
        .also(this::addJavadoc)
        .addParameters(query.parameters.map {
          ParameterSpec.builder(it.name, it.argumentType()).build()
        })
  }

  fun value(): PropertySpec {
    return PropertySpec.builder(query.name, ClassName("", query.name.capitalize()), KModifier.PRIVATE)
        .initializer("${query.name.capitalize()}()")
        .build()
  }
}
