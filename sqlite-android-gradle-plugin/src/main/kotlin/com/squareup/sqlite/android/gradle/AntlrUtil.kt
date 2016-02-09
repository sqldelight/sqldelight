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
package com.squareup.sqlite.android.gradle

import com.squareup.sqlite.android.SQLiteParser.Sql_stmtContext
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

internal fun ParserRuleContext.textWithWhitespace(): String {
  var context = this
  if (context is Sql_stmtContext) {
    context = context.getChild(context.getChildCount() - 1) as ParserRuleContext
  }

  return if (context.start == null || context.stop == null || context.start.startIndex < 0 || context.stop.stopIndex < 0) context.text
  else context.start.inputStream.getText(Interval(context.start.startIndex, context.stop.stopIndex))
}
