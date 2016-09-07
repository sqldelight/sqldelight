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

import android.support.annotation.NonNull;

/** A {@link ColumnAdapter} which maps the enum class {@code T} to a string in the database. */
public final class EnumColumnAdapter<T extends Enum<T>> implements ColumnAdapter<T, String> {
  public static <T extends Enum<T>> EnumColumnAdapter<T> create(Class<T> cls) {
    if (cls == null) throw new NullPointerException("cls == null");
    return new EnumColumnAdapter<>(cls);
  }

  private final Class<T> cls;

  private EnumColumnAdapter(Class<T> cls) {
    this.cls = cls;
  }

  @Override @NonNull public T decode(String databaseValue) {
    return Enum.valueOf(cls, databaseValue);
  }

  @Override public String encode(@NonNull T value) {
    return value.name();
  }
}
