/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight;

import android.annotation.SuppressLint;
import android.arch.persistence.db.SupportSQLiteStatement;
import android.support.annotation.NonNull;

public abstract class SqlDelightStatement implements SupportSQLiteStatement {
  private final String table;
  private final SupportSQLiteStatement program;

  protected SqlDelightStatement(@NonNull String table, @NonNull SupportSQLiteStatement program) {
    this.table = table;
    this.program = program;
  }

  /** The table this statement affects. */
  @NonNull
  public final String getTable() {
    return table;
  }

  @Override public final void execute() {
    program.execute();
  }

  @Override public final int executeUpdateDelete() {
    return program.executeUpdateDelete();
  }

  @Override public final long executeInsert() {
    return program.executeInsert();
  }

  @Override public final long simpleQueryForLong() {
    return program.simpleQueryForLong();
  }

  @Override public final String simpleQueryForString() {
    return program.simpleQueryForString();
  }

  @Override public final void bindNull(int index) {
    program.bindNull(index);
  }

  @Override public final void bindLong(int index, long value) {
    program.bindLong(index, value);
  }

  @Override public final void bindDouble(int index, double value) {
    program.bindDouble(index, value);
  }

  @Override public final void bindString(int index, String value) {
    program.bindString(index, value);
  }

  @Override public final void bindBlob(int index, byte[] value) {
    program.bindBlob(index, value);
  }

  @Override public final void clearBindings() {
    program.clearBindings();
  }

  @SuppressLint("NewApi") // TODO Remove once 'db' version 1.1.0 is released and fixes this.
  @Override public final void close() throws Exception {
    program.close();
  }
}
