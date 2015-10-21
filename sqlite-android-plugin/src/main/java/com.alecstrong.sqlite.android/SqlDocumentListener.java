package com.alecstrong.sqlite.android;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;

public class SqlDocumentListener extends DocumentAdapter {
  private final Document document;

  public SqlDocumentListener(Document document) {
    this.document = document;
  }

  @Override public void documentChanged(DocumentEvent e) {
    System.out.println("ayy lmao");
    SQLiteBaseListener listener = new SQLiteBaseListener();
    super.documentChanged(e);
  }
}
