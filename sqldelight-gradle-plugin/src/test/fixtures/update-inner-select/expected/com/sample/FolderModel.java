package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable FolderModel copy) {
      if (copy != null) {
        this.fid(copy.fid());
        this.total_counter(copy.total_counter());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal fid(long fid) {
      contentValues.put(FID, fid);
      return this;
    }

    public Marshal total_counter(int total_counter) {
      contentValues.put(TOTAL_COUNTER, total_counter);
      return this;
    }
  }

  final class Factory<T extends FolderModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(FolderModel copy) {
      return new Marshal(copy);
    }

    public void update_total_counter_by_fid(Update_total_counter_by_fidStatement statement, long fid) {
      statement.program.bindLong(1, fid);
    }
  }

  final class Update_total_counter_by_fidStatement {
    public static final String table = "folder";

    public final SQLiteStatement program;

    public Update_total_counter_by_fidStatement(SQLiteDatabase database) {
      program = database.compileStatement(""
              + "UPDATE folder SET\n"
              + "total_counter = (SELECT COUNT(*) FROM message WHERE folder.fid=message.fid)\n"
              + "WHERE folder.fid = ?");
    }
  }
}
