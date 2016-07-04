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
package com.squareup.sqldelight.intellij.util

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.moduleDirectory(): VirtualFile? {
  var current = this
  var doubleParent = parent?.parent
  while (doubleParent != null) {
    if (current.isDirectory && current.name == "sqldelight" && doubleParent.name == "src") {
      return doubleParent.parent
    }
    current = current.parent
    doubleParent = doubleParent.parent
  }
  return null
}
