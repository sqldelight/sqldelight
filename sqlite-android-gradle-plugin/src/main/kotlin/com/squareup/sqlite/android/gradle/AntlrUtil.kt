package com.squareup.sqlite.android.gradle

import com.squareup.sqlite.android.SQLiteParser
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