package com.alecstrong.sqlite.android.lang;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class SqliteFile extends PsiFileBase {
	protected SqliteFile(@NotNull FileViewProvider viewProvider) {
		super(viewProvider, SqliteLanguage.INSTANCE);
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
