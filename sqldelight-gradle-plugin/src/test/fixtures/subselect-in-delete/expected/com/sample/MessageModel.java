package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface MessageModel {
  String TABLE_NAME = "message";

  String MID = "mid";

  String FID = "fid";

  String CREATE_TABLE = ""
      + "CREATE TABLE message (\n"
      + "    mid         INTEGER PRIMARY KEY NOT NULL,\n"
      + "    fid         INTEGER NOT NULL\n"
      + ")";

  String DELETE_ORPHANS = ""
      + "DELETE FROM folder WHERE folder.fid IN (\n"
      + "  SELECT folder.fid FROM folder\n"
      + "  LEFT JOIN message ON message.fid=folder.fid\n"
      + ")";

  String DELETE_ORPHANS_2 = ""
      + "DELETE FROM folder WHERE folder.fid IN (\n"
      + "  SELECT folder.fid FROM folder WHERE fid = fid\n"
      + ")";

  long mid();

  long fid();

  interface Creator<T extends MessageModel> {
    T create(long mid, long fid);
  }

  final class Mapper<T extends MessageModel> implements RowMapper<T> {
    private final Factory<T> messageModelFactory;

    public Mapper(Factory<T> messageModelFactory) {
      this.messageModelFactory = messageModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return messageModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable MessageModel copy) {
      if (copy != null) {
        this.mid(copy.mid());
        this.fid(copy.fid());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal mid(long mid) {
      contentValues.put("mid", mid);
      return this;
    }

    public Marshal fid(long fid) {
      contentValues.put("fid", fid);
      return this;
    }
  }

  final class Factory<T extends MessageModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(MessageModel copy) {
      return new Marshal(copy);
    }
  }
}
