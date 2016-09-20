/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.types

import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.model.SqlStmt
import com.squareup.sqldelight.util.parse
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.Calendar


class ArgumentTests {
  private val testFile = File("src/test/data/ArgumentTestData.sq")
  private val parsed = parse(testFile)
  private val symbolTable = SymbolTable() + SymbolTable(parsed, testFile, testFile.path)
  private val status = SqlDelightValidator()
      .validate(testFile.path, parsed, symbolTable) as Status.ValidationStatus.Validated

  @Test
  fun insertValues() {
    assertThat(status.sqlStmts.withName("insert_test"))
        .hasArgumentCount(6)
        .hasArgument(ClassName.get(String::class.java), 1, false)
        .hasArgument(ClassName.get(String::class.java), 2, true)
        .hasArgument(ClassName.get(Calendar::class.java), 3, true)
        .hasArgument(TypeName.BOOLEAN, 4, false)
        .hasArgument(TypeName.DOUBLE.box(), 5, true)
        .hasArgument(TypeName.LONG, 6, false)
  }

  @Test
  fun selectOneAtBack() {
    assertThat(status.sqlStmts.withName("select_one_at_back"))
        .hasArgumentCount(3)
        .hasArgument(ClassName.get(Calendar::class.java), 1, true)
        .hasArgument(ClassName.get(Calendar::class.java), 2, true)
        .hasArgument(ClassName.get(Calendar::class.java), 3, true, "arg3")
  }

  @Test
  fun lateIndex() {
    assertThat(status.sqlStmts.withName("late_index"))
        .hasArgumentCount(3)
        .hasArgument(ClassName.get(Calendar::class.java), 2, true)
        .hasArgument(ClassName.get(Calendar::class.java), 3, true)
        .hasArgument(ClassName.get(Calendar::class.java), 4, true, "arg4")
  }

  @Test
  fun array() {
    assertThat(status.sqlStmts.withName("array"))
        .hasArgumentCount(1)
        .hasArgument(ClassName.get(Calendar::class.java), 1, true, isArray = true)
  }

  private fun assertThat(sqlStmt: SqlStmt) = SqlStmtSubject(sqlStmt)

  private class SqlStmtSubject(
      val sqlStmt: SqlStmt
  ): Subject<SqlStmtSubject, SqlStmt>(Truth.THROW_ASSERTION_ERROR, sqlStmt) {

    fun hasArgument(
        type: TypeName,
        index: Int, nullable: Boolean,
        name: String? = null,
        isArray: Boolean = false
    ): SqlStmtSubject {
      if (sqlStmt.arguments.none {
        (index == it.index)
            && (type == (it.argumentType.comparable?.javaType ?: TypeName.OBJECT))
            && (name == null || name == it.name)
            && (nullable == (it.argumentType.comparable?.nullable ?: true))
            && ((isArray && it.argumentType is ArgumentType.SetOfValues)
            || (!isArray && it.argumentType is ArgumentType.SingleValue))
      }) {
        Assert.fail("No argument at index $index with type $type found in ${sqlStmt.arguments.joinToString(",\n")}")
      }
      return this
    }

    fun hasArgumentCount(count: Int): SqlStmtSubject {
      assertThat(sqlStmt.arguments).hasSize(count)
      return this
    }
  }

  private fun List<SqlStmt>.withName(name: String) = first { it.name == name }
}
