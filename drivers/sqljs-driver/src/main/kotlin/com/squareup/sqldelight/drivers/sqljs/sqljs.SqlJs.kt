@file:JsQualifier("SqlJs")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "EXTERNAL_DELEGATION")
package com.squareup.sqldelight.drivers.sqljs

import org.khronos.webgl.Uint8Array

external interface QueryResults {
    var columns: Array<String>
    var values: Array<Array<dynamic /* Number | String | Uint8Array | Nothing? */>>
}

external interface ParamsObject
external interface Buffer
external interface ParamsCallback
external interface Config

open external class Database() {
    constructor(data: Buffer?)
    constructor(data: Uint8Array?)
    constructor(data: Array<Number>?)
    open fun run(sql: String): Database
    open fun run(sql: String, params: ParamsObject): Database
    open fun run(sql: String, params: Array<dynamic /* Number | String | Uint8Array | Nothing? */>): Database
    open fun exec(sql: String): Array<QueryResults>
    open fun each(sql: String, callback: ParamsCallback, done: () -> Unit)
    open fun each(sql: String, params: ParamsObject, callback: ParamsCallback, done: () -> Unit)
    open fun each(sql: String, params: Array<dynamic /* Number | String | Uint8Array | Nothing? */>, callback: ParamsCallback, done: () -> Unit)
    open fun prepare(sql: String): Statement
    open fun prepare(sql: String, params: ParamsObject): Statement
    open fun prepare(sql: String, params: Array<dynamic /* Number | String | Uint8Array | Nothing? */>): Statement
    open fun export(): Uint8Array
    open fun close()
    open fun getRowsModified(): Number
    open fun create_function(name: String, func: Function<*>)
}

open external class Statement {
    open fun bind(): Boolean
    open fun bind(values: ParamsObject): Boolean
    open fun bind(values: Array<dynamic /* Number | String | Uint8Array | Nothing? */>): Boolean
    open fun step(): Boolean
    open fun get(): Array<dynamic /* Number | String | Uint8Array | Nothing? */>
    open fun get(params: ParamsObject): Array<dynamic /* Number | String | Uint8Array | Nothing? */>
    open fun get(params: Array<dynamic /* Number | String | Uint8Array | Nothing? */>): Array<dynamic /* Number | String | Uint8Array | Nothing? */>
    open fun getColumnNames(): Array<String>
    open fun getAsObject(): ParamsObject
    open fun getAsObject(params: ParamsObject): ParamsObject
    open fun getAsObject(params: Array<dynamic /* Number | String | Uint8Array | Nothing? */>): ParamsObject
    open fun run()
    open fun run(values: ParamsObject)
    open fun run(values: Array<dynamic /* Number | String | Uint8Array | Nothing? */>)
    open fun reset()
    open fun freemem()
    open fun free(): Boolean
}

external interface SqlJsStatic {
    var Database: InitDatabaseJsStatic
    var Statement: InitStatementJsStatic
}

external interface InitDatabaseJsStatic
external interface InitStatementJsStatic

external interface InitSqlJsStatic {
    var default: InitSqlJsStatic /* this */
}
