/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DuplicateSqlIdentifiers {

  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun duplicateQueryName() {
    val result = FixtureCompiler.compileSql("""
      |some_select:
      |SELECT 1;
      |
      |some_select:
      |SELECT 1;
      |""".trimMargin(), tempFolder)

    assertThat(result.errors).contains("Test.sq line 1:0 - Duplicate SQL identifier")
    assertThat(result.errors).contains("Test.sq line 4:0 - Duplicate SQL identifier")
  }
}
