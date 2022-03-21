package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.test.util.FixtureCompiler
import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PgInsertOnConflictTest {
    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun `postgres INSERT DO UPDATE works with 1 column`() {
        val file = FixtureCompiler.parseSql(
            """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT ''
            |);
            |
            |upsertCols:
            |INSERT INTO data
            |VALUES (:id, :c1)
            |ON CONFLICT (id) DO UPDATE SET col1 = :c1;
            """.trimMargin(),
            tempFolder, dialectPreset = DialectPreset.POSTGRESQL
        )

        val insert = file.namedMutators.first()
        val generator = MutatorQueryGenerator(insert)

        Truth.assertThat(generator.function().toString()).isEqualTo(
            """
            |public fun upsertCols(
            |  id: kotlin.Int?,
            |  c1: kotlin.String?
            |): kotlin.Unit {
            |  driver.execute(${insert.id}, ""${'"'}
            |  |INSERT INTO data
            |  |VALUES (?, ?)
            |  |ON CONFLICT (id) DO UPDATE SET col1 = ?
            |  ""${'"'}.trimMargin(), 3) {
            |    bindLong(1, id?.let { it.toLong() })
            |    bindString(2, c1)
            |    bindString(3, c1)
            |  }
            |  notifyQueries(${insert.id}) { emit ->
            |    emit("data")
            |  }
            |}
            |
            """.trimMargin()
        )
    }

    @Test
    fun `postgres INSERT DO UPDATE works with 2 columns`() {
        val file = FixtureCompiler.parseSql(
            """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT '',
            |  col2 TEXT DEFAULT '',
            |  col3 TEXT DEFAULT ''
            |);
            |
            |upsertCols:
            |INSERT INTO data
            |VALUES (:id, :c1, :c2, :c3)
            |ON CONFLICT (id) DO UPDATE SET col1 = :c1, col2 = :c2;
            """.trimMargin(),
            tempFolder, dialectPreset = DialectPreset.POSTGRESQL
        )

        val insert = file.namedMutators.first()
        val generator = MutatorQueryGenerator(insert)

        Truth.assertThat(generator.function().toString()).isEqualTo(
            """
            |public fun upsertCols(
            |  id: kotlin.Int?,
            |  c1: kotlin.String?,
            |  c2: kotlin.String?,
            |  c3: kotlin.String?
            |): kotlin.Unit {
            |  driver.execute(${insert.id}, ""${'"'}
            |  |INSERT INTO data
            |  |VALUES (?, ?, ?, ?)
            |  |ON CONFLICT (id) DO UPDATE SET col1 = ?, col2 = ?
            |  ""${'"'}.trimMargin(), 6) {
            |    bindLong(1, id?.let { it.toLong() })
            |    bindString(2, c1)
            |    bindString(3, c2)
            |    bindString(4, c3)
            |    bindString(5, c1)
            |    bindString(6, c2)
            |  }
            |  notifyQueries(${insert.id}) { emit ->
            |    emit("data")
            |  }
            |}
            |
            """.trimMargin()
        )
    }

    @Test
    fun `postgres INSERT DO UPDATE works with 3 columns`() {
        val file = FixtureCompiler.parseSql(
            """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT '',
            |  col2 TEXT DEFAULT '',
            |  col3 TEXT DEFAULT ''
            |);
            |
            |upsertCols:
            |INSERT INTO data
            |VALUES (:id, :c1, :c2, :c3)
            |ON CONFLICT (id) DO UPDATE SET col1 = :c1, col2 = :c2, col3 = :c3;
            """.trimMargin(),
            tempFolder, dialectPreset = DialectPreset.POSTGRESQL
        )

        val insert = file.namedMutators.first()
        val generator = MutatorQueryGenerator(insert)

        Truth.assertThat(generator.function().toString()).isEqualTo(
            """
            |public fun upsertCols(
            |  id: kotlin.Int?,
            |  c1: kotlin.String?,
            |  c2: kotlin.String?,
            |  c3: kotlin.String?
            |): kotlin.Unit {
            |  driver.execute(${insert.id}, ""${'"'}
            |  |INSERT INTO data
            |  |VALUES (?, ?, ?, ?)
            |  |ON CONFLICT (id) DO UPDATE SET col1 = ?, col2 = ?, col3 = ?
            |  ""${'"'}.trimMargin(), 7) {
            |    bindLong(1, id?.let { it.toLong() })
            |    bindString(2, c1)
            |    bindString(3, c2)
            |    bindString(4, c3)
            |    bindString(5, c1)
            |    bindString(6, c2)
            |    bindString(7, c3)
            |  }
            |  notifyQueries(${insert.id}) { emit ->
            |    emit("data")
            |  }
            |}
            |
            """.trimMargin()
        )
    }
}
