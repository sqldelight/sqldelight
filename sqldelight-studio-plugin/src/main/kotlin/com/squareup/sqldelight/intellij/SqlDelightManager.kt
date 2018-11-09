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
package com.squareup.sqldelight.intellij

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.containers.BidirectionalMap
import com.intellij.util.containers.MultiMap
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.lang.SqlDelightFileViewProvider
import com.squareup.sqldelight.intellij.lang.SqliteFile
import com.squareup.sqldelight.intellij.util.containingParse
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.validation.SqlDelightValidator.Companion.ALL_FILE_DEPENDENCY
import org.antlr.v4.runtime.ParserRuleContext

class SqlDelightManager private constructor() {
  var symbolTable = SymbolTable()
  private val parseTreeMap = BidirectionalMap<SqliteFile, SqliteParser.ParseContext>()
  private val dependencies = MultiMap<Any, SqlDelightFileViewProvider>()

  fun setParseTree(file: SqliteFile, parseContext: SqliteParser.ParseContext) {
    parseTreeMap.remove(file)
    parseTreeMap.put(file, parseContext)
  }

  fun getPsi(parsed: ParserRuleContext) = parseTreeMap.getKeysByValue(parsed.containingParse())
      ?.first()?.findElementAt(parsed.start.startIndex)

  fun setDependencies(fileViewProvider: SqlDelightFileViewProvider, newDependencies: () -> List<Any>) {
    synchronized(dependencies) {
      dependencies.entrySet().forEach {
        it.value.remove(fileViewProvider)
      }
      newDependencies().forEach {
        dependencies.putValue(it, fileViewProvider)
      }
    }
  }

  fun removeFile(
      file: VirtualFile,
      fromEdit: Boolean = false,
      replacementTable: SymbolTable? = null
  ) {
    symbolTable -= file
    if (replacementTable != null) {
      symbolTable += replacementTable
    }
    dependencies.entrySet().forEach {
      it.value.removeAll { it.virtualFile == file }
    }
    triggerDependencyRefresh(file, fromEdit)
    dependencies.remove(file)
  }

  fun triggerDependencyRefresh(file: VirtualFile, fromEdit: Boolean = false) {
    if (fromEdit) {
      (dependencies.get(file) + dependencies.get(ALL_FILE_DEPENDENCY))
          .forEach { it.generateJavaInterface() }
    }
  }

  companion object {
    private val managers = linkedMapOf<Module, SqlDelightManager>()

    fun initManager(module: Module) {
      synchronized(managers) {
        managers.put(module, SqlDelightManager())
      }
    }

    fun disposeManager(module: Module) {
      synchronized(managers) {
        managers.remove(module)
      }
    }

    fun getInstance(module: Module): SqlDelightManager? {
      synchronized(managers) {
        return managers[module]
      }
    }

    fun getInstance(element: PsiElement): SqlDelightManager? {
      try {
        return getInstance(ModuleUtil.findModuleForPsiElement(element) ?: return null)
      } catch (e: Throwable) {
        return null
      }
    }

    fun removeFile(file: VirtualFile) {
      synchronized(managers) {
        managers.values.forEach { manager ->
          manager.removeFile(file, true)

          manager.parseTreeMap.keys
              .filter { it.virtualFile?.name == file.name }
              .forEach { manager.parseTreeMap.remove(it) }
        }
      }
    }
  }
}
