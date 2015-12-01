package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlitePairedBraceMatcher implements PairedBraceMatcher {
  private static final BracePair[] BRACE_PAIRS = new BracePair[] {
      new BracePair(SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteLexer.OPEN_PAR),
          SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteLexer.CLOSE_PAR), false)
  };

  @Override public BracePair[] getPairs() {
    return BRACE_PAIRS;
  }

  @Override public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType,
      @Nullable IElementType contextType) {
    return true;
  }

  @Override public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
