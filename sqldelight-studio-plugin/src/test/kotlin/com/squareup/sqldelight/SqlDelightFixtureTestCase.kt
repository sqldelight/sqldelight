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
package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.squareup.sqldelight.intellij.lang.SqlDelightFileViewProvider
import com.squareup.sqldelight.types.SymbolTable
import java.io.File

abstract class SqlDelightFixtureTestCase : LightPlatformCodeInsightFixtureTestCase() {
  private val sqldelightDir = "main/sqldelight/com/sample"

  abstract val fixtureDirectory: String

  override fun getTestDataPath() = "testData/$fixtureDirectory"

  override fun setUp() {
    super.setUp()
    // Copy the universal files into the fixture directory.
    val filesToLoad = arrayListOf<String>()
    File("testData/main").listFiles().forEach {
      File(testDataPath, "$sqldelightDir/${it.name}")
          .writeText(it.readText().replace(CUSTOM_CARET_REGEX, "")
      )
      filesToLoad.add("$sqldelightDir/${it.name}")
    }

    // Reset the symbol table.
    SqlDelightFileViewProvider.symbolTable = SymbolTable()

    // Load universal files into the IDE.
    myFixture.configureByFiles(*filesToLoad.toTypedArray())
    for (file in filesToLoad) {
      (myFixture.psiManager
          .findFile(myFixture.findFileInTempDir(file))!!.viewProvider as SqlDelightFileViewProvider)
          .generateJavaInterface()
    }
  }

  override fun tearDown() {
    super.tearDown()
    // Remove the source files from the fixture directory.
    File("testData/main").listFiles().forEach {
      File(testDataPath, "$sqldelightDir/${it.name}").delete()
    }
  }

  /**
   * Verify variants at the cursor position specified by the <caret> tag in the input file.
   * The input file is equal to (TEST_NAME).sq
   */
  protected fun doTestVariants(vararg variants: String) {
    myFixture.configureByFile("$sqldelightDir/${getTestName(false)}.sq")
    myFixture.complete(CompletionType.BASIC, 1);
    assertThat(myFixture.lookupElementStrings).containsExactly(*variants)
  }

  /**
   * Verify that a command+b (go to declaration) IDE action at the cursor position specified
   * by the <caret> tag in the input file goes to the given file at the position specified by
   * the caret tag passed in.
   * The input file is equal to (TEST_NAME).sq
   */
  protected fun doTestReference(fileName: String, caret: String) {
    val reference = myFixture.getReferenceAtCaretPosition("$sqldelightDir/${getTestName(false)}.sq")
    assertThat(reference).isNotNull()
    reference!!.resolve()!!.assertThat().isAtPosition(fileName, getPosition(fileName, caret))
  }

  /**
   * Given a fileName and a caret identifier find the index of the text <identifier>. Note that
   * this will ignore other carets when computing the index.
   */
  private fun getPosition(fileName: String, identifier: String): Int {
    val text = File("testData/main", fileName).readText().replace(CUSTOM_CARET_REGEX, {
      if (it.value == "<$identifier>") it.value
      else ""
    })
    return text.indexOf("<$identifier>")
  }

  companion object {
    private val CUSTOM_CARET_REGEX = Regex("<[^<>]*>")
  }
}
