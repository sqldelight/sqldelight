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

@SuppressWarnings("unused") // Used by generated code.
public final class QuestionMarks {
  public static String ofSize(int count) {
    // 1 question mark, count-1 of comma+space+question, and two parenthesis, summed, simplifies to:
    StringBuilder builder = new StringBuilder(count * 3);
    builder.append("(?");
    for (int i = 1; i < count; i++) {
      builder.append(", ?");
    }
    return builder.append(')').toString();
  }

  private QuestionMarks() {
  }
}
