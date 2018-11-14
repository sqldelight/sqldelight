package com.squareup.sqldelight.sqliter.driver

import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.test.Test
import kotlin.test.assertEquals

class DriverStructureTest{
    @Test
    fun emptyHelper(){
        val databaseManager = StructDatabaseManager()
        val helper = SQLiterHelper(databaseManager)
        helper.close()
        assertEquals(0, helper.connectionList.size)
        databaseManager.assertClosed()
    }

    @Test
    fun closeConnection(){
        val databaseManager = StructDatabaseManager()
        val helper = SQLiterHelper(databaseManager)
        helper.getConnection() as SQLiterConnection

        helper.close()
        (helper.connectionList.get(0).databaseConnection as StructDatabaseConnection).assertClosed()
        databaseManager.assertClosed()
    }

    @Test
    fun `all queries closed when connection closed`(){
        val databaseManager = StructDatabaseManager()
        val helper = SQLiterHelper(databaseManager)
        val conn = helper.getConnection()

        val stmt = conn.prepareStatement("select a, b from test", SqlPreparedStatement.Type.SELECT, 0) as SQLiterQuery

        assertEquals(0, stmt.statementList.size)

        val query1 = stmt.executeQuery() as SQLiterCursor
        val query2 = stmt.executeQuery() as SQLiterCursor

        assertEquals(0, stmt.statementList.size)

        query1.close()
        query2.close()

        assertEquals(2, stmt.statementList.size)


        stmt.statementList.forEach {
            (it as StructStatement).assertNotClosed()
        }

        helper.close()

        stmt.statementList.forEach {
            (it as StructStatement).assertClosed()
        }
    }
}