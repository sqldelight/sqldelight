package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.SqldelightParser
import app.cash.sqldelight.core.SqldelightParserUtil
import app.cash.sqldelight.core.lang.psi.FunctionExprMixin
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.SqlDelightPragmaName
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.project.Project
import java.util.ServiceLoader

internal class ParserUtil {
  internal var dialect: Class<out SqlDelightDialect>? = null

  fun initializeDialect(project: Project) {
    val newDialect = SqlDelightProjectService.getInstance(project).dialect
    if (newDialect::class.java != dialect) {
      SqlParserUtil.reset()
      SqldelightParserUtil.reset()

      newDialect.setup()
      ServiceLoader.load(SqlDelightModule::class.java, newDialect::class.java.classLoader).forEach {
        it.setup()
      }
      SqldelightParserUtil.overrideSqlParser()
      dialect = newDialect::class.java

      val currentElementCreation = SqldelightParserUtil.createElement
      SqldelightParserUtil.createElement = {
        when (it.elementType) {
          SqlTypes.FUNCTION_EXPR -> FunctionExprMixin(it)
          SqlTypes.PRAGMA_NAME -> SqlDelightPragmaName(it)
          else -> currentElementCreation(it)
        }
      }

      installMissingColonHint()
    }
  }

  /**
   * A labeled statement continues either with `label:` (named query or execute) or with
   * `label {` (transaction block). When the continuation is neither of those, the grammar's
   * pinned `stmt_identifier_clojure` only reports the missing `{`, which sends users of the
   * much more common `label:` form down the wrong path (see issue #6385).
   *
   * While `stmt_identifier_clojure` is being attempted and has not yet consumed its `{`,
   * register an extra `:` expected-variant right after the label identifier. Error message
   * generation only keeps variants registered at the failing position, so successful parses
   * are unaffected, and the resulting message mentions both possible followers:
   * `':' or '{' expected, got 'SELECT'`.
   */
  private fun installMissingColonHint() {
    val insideClojureLabel = ThreadLocal.withInitial { false }

    val currentStmtIdentifierClojure = SqldelightParserUtil.stmt_identifier_clojure
    SqldelightParserUtil.stmt_identifier_clojure = GeneratedParserUtilBase.Parser { builder, level ->
      insideClojureLabel.set(true)
      try {
        (currentStmtIdentifierClojure
          ?: GeneratedParserUtilBase.Parser { b, l -> SqldelightParser.stmt_identifier_clojure_real(b, l) })
          .parse(builder, level)
      } finally {
        insideClojureLabel.set(false)
      }
    }

    // Once the '{' has been consumed we are parsing regular statements inside the block;
    // identifiers there must not produce the ':' hint.
    val currentStmtClojureStmtList = SqldelightParserUtil.stmt_clojure_stmt_list
    SqldelightParserUtil.stmt_clojure_stmt_list = GeneratedParserUtilBase.Parser { builder, level ->
      val previous = insideClojureLabel.get()
      insideClojureLabel.set(false)
      try {
        (currentStmtClojureStmtList
          ?: GeneratedParserUtilBase.Parser { b, l -> SqldelightParser.stmt_clojure_stmt_list_real(b, l) })
          .parse(builder, level)
      } finally {
        insideClojureLabel.set(previous)
      }
    }

    val currentIdentifier = SqlParserUtil.identifier
    SqlParserUtil.identifier = GeneratedParserUtilBase.Parser { builder, level ->
      val result = (currentIdentifier
        ?: GeneratedParserUtilBase.Parser { b, l -> com.alecstrong.sql.psi.core.SqlParser.identifier_real(b, l) })
        .parse(builder, level)
      if (result && insideClojureLabel.get()) {
        val next = builder.tokenText
        if (next != ":" && next != "{") {
          GeneratedParserUtilBase.addVariant(builder, ":")
        }
      }
      result
    }
  }
}
