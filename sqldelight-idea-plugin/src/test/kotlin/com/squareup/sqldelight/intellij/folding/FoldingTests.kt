/*
 * Copyright (C) 2018 Square, Inc.
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

package com.squareup.sqldelight.intellij.folding

import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class FoldingTests : SqlDelightFixtureTestCase() {
  override val fixtureDirectory = "folding"

  fun testSingleImport() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/SingleImport.sq")
  }

  fun testMultipleImports() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/MultipleImports.sq")
  }

  fun testIncompleteImport() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/IncompleteImport.sq")
  }

  fun testCreateTable() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/CreateTable.sq")
  }

  fun testCreateView() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/CreateView.sq")
  }

  fun testCreateTrigger() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/CreateTrigger.sq")
  }

  fun testCreateIndex() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/CreateIndex.sq")
  }

  fun testStatements() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/Statements.sq")
  }

  fun testAll() {
    myFixture.testFolding("$testDataPath/$sqldelightDir/Player.sq")
  }
}
