package com.squareup.sqldelight.intellij.highlighting

import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class HighlightingTests : SqlDelightFixtureTestCase() {
  fun testComplexExpressionClauseCompilesFine() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE item(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  packageName TEXT NOT NULL,
      |  className TEXT NOT NULL,
      |  deprecated INTEGER NOT NULL DEFAULT 0,
      |  link TEXT NOT NULL,
      |
      |  UNIQUE (packageName, className)
      |);
      |
      |INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)
      |;
      |
      |UPDATE item
      |SET deprecated = ?3,
      |    link = ?4
      |WHERE packageName = ?1
      |  AND className = ?2
      |;
      |
      |SELECT COUNT(id)
      |FROM item
      |;
      |
      |selectItems:
      |SELECT item.*
      |FROM item
      |WHERE className LIKE '%' || ?1 || '%' ESCAPE '\'
      |ORDER BY
      |  -- deprecated classes are always last
      |  deprecated ASC,
      |  CASE
      |    -- exact match
      |    WHEN className LIKE ?1 ESCAPE '\' THEN 1
      |    -- prefix match with no nested type
      |    WHEN className LIKE ?1 || '%' ESCAPE '\' AND instr(className, '.') = 0 THEN 2
      |    -- exact match on nested type
      |    WHEN className LIKE '%.' || ?1 ESCAPE '\' THEN 3
      |    -- prefix match (allowing nested types)
      |    WHEN className LIKE ?1 || '%' ESCAPE '\' THEN 4
      |    -- prefix match on nested type
      |    WHEN className LIKE '%.' || ?1 || '%' ESCAPE '\' THEN 5
      |    -- infix match
      |    ELSE 6
      |  END ASC,
      |  -- prefer "closer" matches based on length
      |  length(className) ASC,
      |  -- alphabetize to eliminate any remaining non-determinism
      |  packageName ASC,
      |  className ASC
      |LIMIT 50
      |;
      """.trimMargin())

    myFixture.checkHighlighting()
  }
}
