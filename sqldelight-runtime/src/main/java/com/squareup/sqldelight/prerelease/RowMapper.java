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
package com.squareup.sqldelight.prerelease;

import android.database.Cursor;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

/** Creates instances of {@code T} from rows in a {@link Cursor}. */
public interface RowMapper<T> {
  /**
   * Return an instance of {@code T} corresponding to the values of the current positioned row of
   * {@code cursor}.
   */
  @CheckResult @NonNull T map(@NonNull Cursor cursor);
}
