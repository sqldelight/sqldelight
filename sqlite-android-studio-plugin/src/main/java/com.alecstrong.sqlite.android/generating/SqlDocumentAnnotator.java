package com.alecstrong.sqlite.android.generating;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.alecstrong.sqlite.android.SqliteCompiler.Status;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlDocumentAnnotator extends ExternalAnnotator<TableGenerator, Status<ASTNode>> {
  private final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
  private final SqliteCompiler<ASTNode> sqliteCompiler = new SqliteCompiler<ASTNode>();

  @Nullable
  @Override
  public TableGenerator collectInformation(@NotNull PsiFile file) {
    return TableGenerator.create(file);
  }

  @Nullable
  @Override
  public Status<ASTNode> doAnnotate(TableGenerator tableGenerator) {
    Status<ASTNode> result = sqliteCompiler.write(tableGenerator);
    VirtualFile file = localFileSystem.findFileByIoFile(tableGenerator.getOutputDirectory());
    if (file != null) file.refresh(true, true);
    return result;
  }

  @Override
  public void apply(@NotNull PsiFile file, Status<ASTNode> status,
      @NotNull AnnotationHolder holder) {
    if (status != null && status.result == Status.Result.FAILURE) {
      holder.createErrorAnnotation(
          status.originatingElement == null ? file.getNode() : status.originatingElement,
          status.errorMessage);
    }
    super.apply(file, status, holder);
  }
}
