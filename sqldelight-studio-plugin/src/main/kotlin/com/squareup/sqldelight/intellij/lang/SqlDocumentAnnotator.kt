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
package com.squareup.sqldelight.intellij.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.Status

internal class SqlDocumentAnnotator : ExternalAnnotator<Status?, Status?>() {
  override fun collectInformation(file: PsiFile) = (file as? SqliteFile)?.status
  override fun doAnnotate(status: Status?) = status
  override fun apply(file: PsiFile, status: Status?, holder: AnnotationHolder) {
    when (status) {
      is Status.Failure -> {
        holder.createErrorAnnotation(TextRange(status.originatingElement.start.startIndex,
            status.originatingElement.stop.stopIndex + 1), status.errorMessage)
      }
      is Status.ValidationStatus.Invalid -> {
        for (error in status.errors) {
          if (error.originatingElement.start.startIndex > error.originatingElement.stop.stopIndex) {
            // This can happen if antlr threw an exception parsing. It should happen rarely or not
            // at all but if it does just error the whole file.
            holder.createErrorAnnotation(file, error.errorMessage)
          } else {
            holder.createErrorAnnotation(TextRange(error.originatingElement.start.startIndex,
                error.originatingElement.stop.stopIndex + 1), error.errorMessage)
          }
        }
      }
    }
  }
}
