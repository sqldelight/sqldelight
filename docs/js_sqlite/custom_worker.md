# Implementing a Custom Worker

A SQLDelight web worker is script that can accept incoming messages from the web worker driver, 
execute some SQL operations using the incoming message, and then respond accordingly with any query
results.

Web workers are most easily implemented in plain JavaScript as they are relatively short and simple
scripts.

## Incoming Messages

The web worker driver message format allows SQLDelight to communicate with a worker implementation
in a generic way that isn't tied to a specific SQL dialect or implementation. Every message contains
an `action` property that specifies one of four actions.

### `exec`

This action indicates that the worker should execute some SQL statement attached to the message and
respond with the result of the SQL query. The message will contain a `sql` property containing the
SQL statement to execute, along with a `params` array that contains the parameters that are to be
bound to the statement.

Example message:
```json
{
  "id": 5,
  "action": "exec",
  "sql": "SELECT column_a, column_b FROM some_table WHERE column_a = ?;",
  "params": ["value"]
}
```

### `begin_transaction`

Tells the worker that it should begin a transaction.

Example message:
```json
{
  "id": 2,
  "action": "begin_transaction"
}
```

### `end_transaction`

Tells the worker that it should end the current transaction.

Example message:
```json
{
  "id": 3,
  "action": "end_transaction"
}
```

### `rollback_transaction`

Tells the worker to rollback the current transaction.

Example message:
```json
{
  "id": 8,
  "action": "rollback_transaction"
}
```

## Responding to Messages

Every incoming message contains an `id` property which is a unique integer for that message.
When responding to a message, the worker implementation must include this `id` value in the response
message. This is used by the web worker driver to correctly handle the response.

### The `results` Property

A response message should also contain a `results` property. This is used to communicate the results
of some SQL execution, particularly for the result set of a query. The `results` property should be
an array representing the _rows_ of results, where each entry is an array representing the _columns_
in the result set.

For example, a response to the `exec` message above could be:

```json
{
  "id": 5,
  "results": [
    ["value", "this is the content of column_b"],
    ["value", "this is a different row"]
  ]
}
```

For a SQL statement that does not return a result set, the `results` value should contain a single row/column with a number representing the number of rows that were affected by the execution of the statement.

```json
{
  "id": 10,
  "results": [ [1] ]
}
```

## Examples

* [SQLDelight's SQL.js Worker](https://github.com/cashapp/sqldelight/blob/master/drivers/web-worker-driver/sqljs/sqljs.worker.js)
