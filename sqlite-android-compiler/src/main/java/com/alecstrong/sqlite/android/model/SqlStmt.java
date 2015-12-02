package com.alecstrong.sqlite.android.model;

import com.google.common.base.CaseFormat;
import java.util.List;

public final class SqlStmt<T> extends SqlElement<T> {
  private final String identifier;

  public final String stmt;

  public SqlStmt(String identifier, String stmt, int startOffset, List<Replacement> allReplacements,
      T originatingElement) {
    super(originatingElement);
    this.identifier = identifier;

    StringBuilder stmtBuilder = new StringBuilder("\n");
    int nextOffset = 0;
    for (Replacement replacement : allReplacements) {
      if (replacement.startOffset <= startOffset
          || replacement.endOffset >= startOffset + stmt.length()) {
        continue;
      }

      stmtBuilder.append(stmt.substring(nextOffset, replacement.startOffset - startOffset))
          .append(replacement.replacementText);
      nextOffset = replacement.endOffset - startOffset;
    }
    stmtBuilder.append(stmt.substring(nextOffset, stmt.length()));
    this.stmt = stmtBuilder.toString();
  }

  public static String fieldName(String identifier) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, identifier);
  }

  public String getIdentifier() {
    return fieldName(identifier);
  }

  public static final class Replacement {
    final int startOffset;
    final int endOffset;
    final String replacementText;

    public Replacement(int startOffset, int endOffset, String replacementText) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.replacementText = replacementText;
    }
  }
}
