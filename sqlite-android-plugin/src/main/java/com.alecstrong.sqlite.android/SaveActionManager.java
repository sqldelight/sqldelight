package com.alecstrong.sqlite.android;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.LinkedHashMap;
import java.util.Map;

public class SaveActionManager extends FileDocumentManagerAdapter {
  private static final String FILETYPE = ".sql";

  private final Map<Document, SqlDocumentListener> documentListeners = new LinkedHashMap<>();

  @Override public void fileContentLoaded(VirtualFile file, Document document) {
    if (file.getPath().endsWith(FILETYPE) && !documentListeners.containsKey(document)) {
      SqlDocumentListener documentListener = new SqlDocumentListener(document);
      document.addDocumentListener(documentListener);
      documentListeners.put(document, documentListener);
    }
    super.fileContentLoaded(file, document);
  }
}
