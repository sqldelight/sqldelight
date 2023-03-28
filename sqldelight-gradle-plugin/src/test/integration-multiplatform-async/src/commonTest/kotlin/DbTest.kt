import app.sqltest.shared.common.data.SharedDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DbTest : MultiPlatformTest() {

    @Test
    fun testDb() = runTest {
        val driver = getTestSqlDriverFactory()
        val db = SharedDatabase(driver)
        db.invoke {
            casesQueries
                .all()
                .executeAsList()
        }
    }

}
