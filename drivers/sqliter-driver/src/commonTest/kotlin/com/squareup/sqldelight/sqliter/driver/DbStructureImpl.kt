package com.squareup.sqldelight.sqliter.driver

import co.touchlab.sqliter.*
import co.touchlab.stately.concurrency.AtomicBoolean
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructDatabaseManager():DatabaseManager, Close by CloseImpl(){
    override fun createConnection(): DatabaseConnection {
        return StructDatabaseConnection()
    }
}

class StructDatabaseConnection():DatabaseConnection, Close by CloseImpl(){
    override fun beginTransaction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createStatement(sql: String): Statement {
        return StructStatement()
    }

    override fun endTransaction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTransactionSuccessful() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class StructStatement():Statement, Close by CloseImpl(){
    override fun bindBlob(index: Int, value: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindDouble(index: Int, value: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindLong(index: Int, value: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindNull(index: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindParameterIndex(paramName: String): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bindString(index: Int, value: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeInsert(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun executeUpdateDelete(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun finalizeStatement() {
        close()
    }

    override fun query(): Cursor {
        return StructCursor()
    }

    override fun reset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class StructCursor():Cursor, Close by CloseImpl(){
    override val columnCount: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val columnNames: Map<String, Int>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun columnName(index: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBytes(index: Int): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDouble(index: Int): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLong(index: Int): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getString(index: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getType(index: Int): FieldType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isNull(index: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun next(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

internal interface Close{
    fun close()
    fun assertClosed()
    fun assertNotClosed()
}

internal class CloseImpl:Close{
    private val closed = AtomicBoolean(false)
    override fun close(){
        closed.value = true
    }

    override fun assertClosed(){
        assertTrue(closed.value)
    }

    override fun assertNotClosed(){
        assertFalse (closed.value)
    }
}
