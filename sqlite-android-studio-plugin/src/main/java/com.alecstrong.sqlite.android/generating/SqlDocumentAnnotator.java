package com.alecstrong.sqlite.android.generating;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.alecstrong.sqlite.android.SqliteCompiler.Status;
import com.alecstrong.sqlite.android.lang.SqliteFile;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlDocumentAnnotator
    extends ExternalAnnotator<TableGenerator, SqlDocumentAnnotator.Generation> {
  private final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
  private final SqliteCompiler<ASTNode> sqliteCompiler = new SqliteCompiler<ASTNode>();

  @Nullable
  @Override
  public TableGenerator collectInformation(@NotNull PsiFile file) {
    return TableGenerator.create(file);
  }

  @Nullable
  @Override
  public Generation doAnnotate(TableGenerator tableGenerator) {
    Status<ASTNode> status = sqliteCompiler.write(tableGenerator);
    VirtualFile file = localFileSystem.findFileByIoFile(tableGenerator.getOutputDirectory());
    if (file != null) file.refresh(false, true);
    VirtualFile generatedFile = file == null ? null : file.findFileByRelativePath(
        tableGenerator.packageDirectory() + "/" + tableGenerator.fileName());
    return new Generation(status, generatedFile);
  }

  @Override
  public void apply(@NotNull PsiFile file, Generation generation,
      @NotNull AnnotationHolder holder) {
    if (generation.status == null) return;
    switch (generation.status.result) {
      case FAILURE:
        holder.createErrorAnnotation(generation.status.originatingElement == null ? file.getNode()
            : generation.status.originatingElement, generation.status.errorMessage);
        break;
      case SUCCESS:
        if (generation.generatedFile == null) return;
        ((SqliteFile) file).setGeneratedFile(file.getManager().findFile(generation.generatedFile));
        break;
    }
  }

  static final class Generation {
    private final Status<ASTNode> status;
    private final VirtualFile generatedFile;

    private Generation(Status<ASTNode> status, VirtualFile generatedFile) {
      this.status = status;
      this.generatedFile = generatedFile;
    }
  }
}
