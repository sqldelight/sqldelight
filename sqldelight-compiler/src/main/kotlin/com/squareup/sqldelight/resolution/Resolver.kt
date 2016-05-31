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
package com.squareup.sqldelight.resolution

import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.types.Value
import java.util.LinkedHashSet

/**
 * Takes as input SQL which evaluates to a result set. After resolving the SQL, the resolver
 * is passed to a validator who validates that level of SQL. Examples:
 *
 * SELECT *
 * FROM (
 *   SELECT column1
 *   FROM test
 *   WHERE column2 = 'stuff'
 * );
 *
 * 1. Base resolver is asked to resolve the top level select statement.
 * 2. Resolver recursively resolves the second level select statement.
 * 3. Resolver resolves "test" to be the set of "test.column1" and "test.column2" and
 *    adds these values to its own scope.
 * 4. Resolver passes itself to a new SelectStmtValidator by calling "validate"
 *    with itself and the second level select statement.
 * 5. Resolver then returns a result set using the result columns specified. The result
 *    will be "test.column1".
 * 6. The recursive call from step 2 returns with "test.column1"
 * 7. Resolver passes itself to a new SelectStmtValidator by calling "validate"
 *    with itself and the top level select statement.
 * 8. Resolver then returns a result set using the result columns specified (in this case *).
 *    The result will be "test.column1".
 * (9) Presumably this was called from a task which is generating the mapper-per-query, so
 *     the top most result set will be passed to the compiler to create that mapper.
 *
 * The important distinction is that the resolver is stateful and validators are stateless.
 * The resolver modifies its scope while running and uses that to validate SQL.
 *
 * Both the resolver and the validator can return different types of errors. Validators will always
 * return errors that are independent of state (such as using the DISTINCT keyword in a
 * non-aggregate function). Resolvers will always return errors that depend on state (trying to
 * return a result column that doesnt exist).
 *
 */
data class Resolver(
    /**
     * Symbol Table containing all the available tables this resolver can select from.
     */
    internal val symbolTable: SymbolTable,

    /**
     * Dependencies are a way of communicating to the caller which table tags were used in
     * resolution. In the IntelliJ plugin, table tags are virtual files, so this translates to
     * "which files were used in resolution".
     */
    internal val dependencies: LinkedHashSet<Any> = linkedSetOf<Any>(),

    /**
     * Values that have been provided by a parent Resolver. This is exclusively used when
     * resolving as part of an expression. Example:
     *
     * SELECT *
     * FROM test AS test1
     * WHERE 'sup' IN (
     *   SELECT *
     *   FROM test AS test2
     *   WHERE test2.some_id = test1.some_id
     * );
     *
     * In this example 'test' resolves to 'test.some_id'. Normally resolving the
     * expression select statement would fail with "no table named test1" but because
     * we scope in the current resolution (in this case the resolution of "test" aliased as test1)
     * it is able to resolve "test1.some_id".
     */
    internal val scopedValues: List<Value> = emptyList(),

    /**
     * The offset of an element we wish to know the source of. Note that if we ever change contexts
     * as part of resolution (for example if we need to resolve a view as part of a select's
     * resolution) then this cannot be passed to the resolver being used for the new context.
     * This gets used by the IntelliJ plugin to perform its own reference resolution.
     */
    internal val elementToFind: Int? = null,

    /**
     * Used to prevent cyclic view resolution.
     */
    internal val currentlyResolvingViews: LinkedHashSet<String> = linkedSetOf<String>()
) {

  /**
   * @return a resolver with the with statement's tables included in the symbol table.
   */
  internal fun withResolver(with: SqliteParser.With_clauseContext) =
      copy(with.common_table_expression().fold(symbolTable, { symbolTable, commonTable ->
        symbolTable + SymbolTable(commonTable, commonTable)
      }))

}