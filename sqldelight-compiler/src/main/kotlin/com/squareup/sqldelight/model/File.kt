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
package com.squareup.sqldelight.model

import com.squareup.javapoet.ClassName
import com.squareup.sqldelight.SqliteCompiler
import java.io.File

fun String.relativePath(separatorChar: Char): List<String> {
  val parts = split(separatorChar)
  for (i in 2..parts.size) {
    if (parts[i - 2] == "src" && parts[i] == "sqldelight") {
      return parts.subList(i + 1, parts.size)
    }
  }
  throw IllegalStateException("Files must be organized like src/main/sqldelight/...")
}

fun String.pathPackage() = split(File.separatorChar).dropLast(1).joinToString(".")
fun String.pathFileName() = substringAfterLast(File.separatorChar).substringBefore('.')
fun String.pathAsType() = ClassName.get(pathPackage(), SqliteCompiler.interfaceName(pathFileName()))
