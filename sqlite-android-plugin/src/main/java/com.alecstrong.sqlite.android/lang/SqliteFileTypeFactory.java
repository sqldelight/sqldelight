package com.alecstrong.sqlite.android.lang;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class SqliteFileTypeFactory extends FileTypeFactory {
	@Override
	public void createFileTypes(FileTypeConsumer consumer) {
		consumer.consume(SqliteFileType.INSTANCE, SqliteFileType.DEFAULT_EXTENSION);
	}
}
