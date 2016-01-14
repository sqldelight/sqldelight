package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SqliteCompiler
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

public class SqliteFileTypeFactory : FileTypeFactory() {
  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(SqliteFileType.INSTANCE, SqliteCompiler.FILE_EXTENSION)
  }
}
