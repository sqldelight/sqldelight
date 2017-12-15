/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.internal;

import java.util.AbstractSet;
import java.util.Iterator;

/** A simple set of tables optimized for calls to {@link #contains} and {@link #iterator} */
public final class TableSet extends AbstractSet<String> {
  private final String[] values;

  public TableSet(String... values) {
    this.values = values;
  }

  @Override public boolean contains(Object o) {
    for (String value : values) {
      if (value.equals(o)) {
        return true;
      }
    }
    return false;
  }

  @Override public Iterator<String> iterator() {
    return new TableIterator(values);
  }

  @Override public int size() {
    return values.length;
  }

  private static final class TableIterator implements Iterator<String> {
    private final String[] values;
    private int i;

    TableIterator(String[] values) {
      this.values = values;
    }

    @Override public boolean hasNext() {
      return i < values.length;
    }

    @Override public String next() {
      return values[i++];
    }
  }
}
