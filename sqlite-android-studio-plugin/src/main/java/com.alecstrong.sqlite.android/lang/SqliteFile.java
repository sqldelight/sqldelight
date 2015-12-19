package com.alecstrong.sqlite.android.lang;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class SqliteFile extends PsiFileBase {
	private PsiFile generatedFile;

	protected SqliteFile(@NotNull FileViewProvider viewProvider) {
		super(viewProvider, SqliteLanguage.INSTANCE);
	}

	public void setGeneratedFile(PsiFile generatedFile) {
		this.generatedFile = generatedFile;
	}

	public PsiFile getGeneratedFile() {
		return generatedFile;
	}

	@NotNull
	@Override
	public FileType getFileType() {
		return SqliteFileType.INSTANCE;
	}

	@Override
	public String toString() {
		return "SQLite file";
	}
}
