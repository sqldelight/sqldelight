package com.sample;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import java.lang.Override;
import java.lang.String;

public interface FolderModel {
  String TABLE_NAME = "folder";

  String FID = "fid";

  String TOTAL_COUNTER = "total_counter";

  String CREATE_TABLE = ""
      + "CREATE TABLE folder (\n"
      + "    fid INTEGER PRIMARY KEY NOT NULL,\n"
      + "    total_counter INTEGER NOT NULL\n"
      + ")";

  long fid();

  int total_counter();

  interface Creator<T extends FolderModel> {
    T create(long fid, int total_counter);
  }

  final class Mapper<T extends FolderModel> implements RowMapper<T> {
    private final Factory<T> folderModelFactory;

    public Mapper(Factory<T> folderModelFactory) {
      this.folderModelFactory = folderModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return folderModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getInt(1)
      );
    }
  }

  final class Factory<T extends FolderModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Update_total_counter_by_fid extends SqlDelightCompiledStatement.Update {
    public Update_total_counter_by_fid(SQLiteDatabase database) {
      super("folder", database.compileStatement(""
              + "UPDATE folder SET\n"
              + "total_counter = (SELECT COUNT(*) FROM message WHERE folder.fid=message.fid)\n"
              + "WHERE folder.fid = ?"));
    }

    public void bind(long fid) {
      program.bindLong(1, fid);
    }
  }
}
