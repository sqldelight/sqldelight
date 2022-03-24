package app.cash.sqldelight.dialects.mysql

import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.test.fixtures.compileFile
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class ColumnOrderingTest {
  @Before
  fun before() {
    File("build/tmp").deleteRecursively()
  }

  @After
  fun after() {
    SqlParserUtil.reset()
    File("build/tmp").deleteRecursively()
  }

  @Test fun `tables works correctly for include all`() {
    MySqlDialect().setup()
    compileFile(
      """
      |CREATE TABLE test1 (
      |  id1 BIGINT NOT NULL,
      |  id2 BIGINT NOT NULL,
      |  id3 BIGINT NOT NULL,
      |  id4 BIGINT NOT NULL,
      |  id5 BIGINT NOT NULL,
      |  id6 BIGINT NOT NULL,
      |  id7 BIGINT NOT NULL,
      |  id8 BIGINT NOT NULL,
      |  id9 BIGINT NOT NULL,
      |  id10 BIGINT NOT NULL
      |);
      |
      |ALTER TABLE test1
      |  ADD COLUMN new11 BIGINT FIRST,
      |  ADD COLUMN new12 BIGINT AFTER id7,
      |  ADD COLUMN new13 BIGINT,
      |  CHANGE COLUMN id2 new14 BIGINT FIRST,
      |  CHANGE COLUMN id4 new15 BIGINT AFTER id8,
      |  CHANGE COLUMN id1 new16 BIGINT,
      |  MODIFY COLUMN id5 BIGINT FIRST,
      |  MODIFY COLUMN id6 BIGINT AFTER id9,
      |  MODIFY COLUMN id7 VARCHAR(8);
    """.trimMargin(),
      "1.s"
    )
    val file = compileFile(
      """
      |SELECT *
      |FROM test1;
    """.trimMargin(),
      "test.s"
    )

    assertThat(
      file.sqlStmtList!!.stmtList.first().compoundSelectStmt!!.queryExposed()
        .flatMap { it.columns }
        .map { it.element.text }
    ).containsExactly(
      "id5",
      "new14",
      "new11",
      "new16",
      "id3",
      "id7",
      "new12",
      "id8",
      "new15",
      "id9",
      "id6",
      "id10",
      "new13"
    ).inOrder()
  }
}
