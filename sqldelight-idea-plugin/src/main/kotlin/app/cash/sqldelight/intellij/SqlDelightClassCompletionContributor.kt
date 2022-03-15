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
package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.JavaTypeMixin
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil.findReferenceOrAlphanumericPrefix
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import kotlin.math.max

class SqlDelightClassCompletionContributor : JavaClassNameCompletionContributor() {
  private val insertHandler = AutoImportInsertionHandler()

  override fun fillCompletionVariants(
    parameters: CompletionParameters,
    resultSet: CompletionResultSet
  ) {
    if (parameters.position.getNonStrictParentOfType<JavaTypeMixin>() == null) return

    val result = resultSet.withPrefixMatcher(findReferenceOrAlphanumericPrefix(parameters))
    val prefixMatcher = result.prefixMatcher
    addAllClasses(parameters, parameters.invocationCount <= 1, prefixMatcher) { lookupElement ->
      if (lookupElement is JavaPsiClassReferenceElement) {
        lookupElement.setInsertHandler(insertHandler)
      }
      resultSet.addElement(lookupElement)
    }

    val file = parameters.originalFile
    val project = file.project
    val module = ModuleUtil.findModuleForFile(file)
    val scope = if (module != null) {
      GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    } else {
      GlobalSearchScope.allScope(project)
    }
    getKotlinClasses(project, prefixMatcher, scope)
      .forEach { classOrObject: KtClassOrObject ->
        val tailText = if (classOrObject.fqName != null) {
          (" ${classOrObject.fqName!!.asString().substringBeforeLast('.')}")
        } else {
          ""
        }
        val elementBuilder = LookupElementBuilder.createWithIcon(classOrObject)
          .withTailText(tailText)
          .withInsertHandler(insertHandler)
        result.addElement(elementBuilder)
      }
  }

  private fun getKotlinClasses(
    project: Project,
    prefixMatcher: PrefixMatcher,
    scope: GlobalSearchScope
  ): List<KtClassOrObject> {
    val index = KotlinFullClassNameIndex.getInstance()
    return index.getAllKeys(project)
      .filter { fqName ->
        ProgressManager.checkCanceled()
        val substringAfterLast = fqName.substringAfterLast('.')
        prefixMatcher.prefixMatches(substringAfterLast)
      }
      .flatMap { fqName ->
        index[fqName, project, scope]
      }
      .filterNotNull()
  }
}

private class AutoImportInsertionHandler : InsertHandler<LookupElement> {
  override fun handleInsert(context: InsertionContext, item: LookupElement) {
    val qname = when (val element = item.psiElement) {
      is PsiClass -> element.qualifiedName
      is KtClassOrObject -> element.fqName?.asString()
      else -> null
    } ?: return

    val imports = (context.file as SqlDelightFile).sqlStmtList
      ?.findChildrenOfType<SqlDelightImportStmt>().orEmpty()
    val ref = imports.map { it.javaType }.find { it.textMatches(qname) }
    val refEnd = context.trackOffset(context.tailOffset, false)

    if (ref == null) {
      if (imports.isEmpty()) {
        context.document.insertString(0, "import $qname;\n\n")
      } else {
        context.insertAndOrganizeImports(imports, "import $qname;")
      }
    }

    context.tailOffset = context.getOffset(refEnd)

    context.commitDocument()
  }

  private fun InsertionContext.insertAndOrganizeImports(
    imports: Collection<SqlDelightImportStmt>,
    newImport: String
  ) {
    val newImports = arrayListOf(newImport)
    var endOffset = 0
    for (importElement in imports) {
      newImports.add(importElement.text)
      endOffset = max(endOffset, importElement.textOffset + importElement.textLength)
    }
    document.replaceString(0, endOffset, newImports.sorted().joinToString("\n"))
  }
}
