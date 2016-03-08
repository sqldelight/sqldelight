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
package com.squareup.sqldelight.psi

import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.squareup.sqldelight.SqliteParser.RULE_java_type_name
import com.squareup.sqldelight.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqldelight.util.elementType
import com.squareup.sqldelight.util.findFirstParent

class ClassNameElement(type: IElementType, text: CharSequence) : LeafPsiElement(type, text) {
  override fun getReference() =
      if (findFirstParent { element -> element is ASTWrapperPsiElement
          && element.elementType === RULE_ELEMENT_TYPES[RULE_java_type_name] } != null) {
        ClassNameElementRef(this, text)
      } else null
}
