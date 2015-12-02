package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingRegistry;
import java.nio.charset.Charset;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqliteFileType extends LanguageFileType {
	public static final LanguageFileType INSTANCE = new SqliteFileType();
	@NonNls public static final String DEFAULT_EXTENSION = SqliteCompiler.getFileExtension();

	private SqliteFileType() {
		super(SqliteLanguage.INSTANCE);
	}

	@Override
	@NotNull
	public String getName() {
		return "Sqlite";
	}

	@Override
	@NotNull
	public String getDescription() {
		return "Sqlite";
	}

	@Override
	@NotNull
	public String getDefaultExtension() {
		return DEFAULT_EXTENSION;
	}

	@Nullable
	@Override
	public Icon getIcon() {
		return Icons.SQLITE;
	}

	@Override
	public String getCharset(@NotNull VirtualFile file, @NotNull final byte[] content) {
		Charset charset = EncodingRegistry.getInstance().getDefaultCharsetForPropertiesFiles(file);
		if (charset == null) {
			charset = CharsetToolkit.getDefaultSystemCharset();
		}
		return charset.name();
	}
}
