package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.SqliteCompiler.Status;
import com.alecstrong.sqlite.android.model.Table;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlDocumentAnnotator extends ExternalAnnotator<Table<ASTNode>, Status<ASTNode>> {
	private final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
	private final SqliteCompiler<ASTNode> sqliteCompiler = new SqliteCompiler<ASTNode>();
	private final TableGenerator tableGenerator = new TableGenerator();

	@Nullable
	@Override
	public Table<ASTNode> collectInformation(@NotNull PsiFile file) {
		return tableGenerator.generateTable(file);
	}

	@Nullable
	@Override
	public Status<ASTNode> doAnnotate(Table<ASTNode> table) {
		if (table != null) {
			Status<ASTNode> result = sqliteCompiler.write(table);
			VirtualFile file = localFileSystem.findFileByIoFile(table.getOutputDirectory());
			if (file != null) file.refresh(true, true);
			return result;
		}
		return null;
	}

	@Override
	public void apply(@NotNull PsiFile file, Status<ASTNode> status, @NotNull AnnotationHolder holder) {
		if (status != null && status.result == Status.Result.FAILURE) {
			holder.createErrorAnnotation(status.originatingElement, status.errorMessage);
		}
		super.apply(file, status, holder);
	}
}
