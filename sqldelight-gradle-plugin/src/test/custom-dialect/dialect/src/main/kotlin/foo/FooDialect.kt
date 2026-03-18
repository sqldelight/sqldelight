package foo

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTypeResolver
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

class FooDialect : SqlDelightDialect by SqliteDialect() {
  override fun typeResolver(parentResolver: TypeResolver) = CustomResolver(parentResolver)

  class CustomResolver(private val parentResolver: TypeResolver) : TypeResolver by SqliteTypeResolver(parentResolver) {
    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
      return when (functionExpr.functionName.text.lowercase()) {
        "foo" -> encapsulatingType(functionExpr.exprList, ExtensionType).asNonNullable()
        else -> parentResolver.functionType(functionExpr)
      }
    }

    override fun definitionType(typeName: SqlTypeName): IntermediateType {
      return when (typeName) {
        else -> IntermediateType(ExtensionType)
      }
    }
  }

  private object ExtensionType : DialectType {
    override val javaType: TypeName = ClassName("kotlin.time", "Duration")
    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
      return CodeBlock.builder()
        .add(
          "$cursorName.getLong($columnIndex)?.%M(%M)",
          MemberName("kotlin.time", "toDuration", isExtension = true),
          MemberName("kotlin.time.DurationUnit", "SECONDS"),
        )
        .build()
    }

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
      return CodeBlock.of("""TODO("Not yet implemented")""")
    }
  }
}
