package foo

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.sqlite_3_18.*
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import org.sqlite.*
import java.io.*
import java.sql.*

class FooDialect : SqlDelightDialect by SqliteDialect() {
  override val connectionManager: ConnectionManager = object: ConnectionManager by SqliteConnectionManager() {
    override fun getConnection(connectionProperties: ConnectionManager.ConnectionProperties): Connection {
      return SQLiteDataSource().apply {
        url = connectionProperties.serializedProperties
        setLoadExtension(true)
        val projectRoot = System.getenv("projectRoot")
        val ext = File(projectRoot, "build/sqlitetokenizer/libsqlite-fts5-synonym-tokenizer.dylib")
        connection.prepareStatement("SELECT load_extension('${ext.absolutePath}', 'sqlite3_fts5_synonym_tokenizer_init');")
          .execute()
      }.connection
    }
  }
  
  override fun typeResolver(parentResolver: TypeResolver) = CustomResolver(parentResolver)

  class CustomResolver(private val parentResolver: TypeResolver) : TypeResolver by SqliteTypeResolver(parentResolver) {
    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
      return when (functionExpr.functionName.text.lowercase()) {
        "foo" -> encapsulatingType(functionExpr.exprList, ExtensionType).asNonNullable()
        else -> parentResolver.functionType(functionExpr)
      }
    }

    override fun definitionType(typeName: SqlTypeName): IntermediateType {
      return when (typeName) { else -> IntermediateType(ExtensionType) }
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

    override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
      return CodeBlock.of("""TODO("Not yet implemented")""")
    }
  }
}
