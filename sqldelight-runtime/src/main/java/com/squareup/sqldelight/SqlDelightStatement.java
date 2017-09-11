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

import java.util.Set;

public final class SqlDelightStatement {
  public final String statement;
  public final String[] args;
  /** A set of the tables this statement observes. */
  public final Set<String> tables;

  public SqlDelightStatement(String statement, String[] args, Set<String> tables) {
    this.statement = statement;
    this.args = args;
    this.tables = tables;
  }
}
