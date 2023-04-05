import app.sqltest.shared.common.data.SqlDriverFactoryProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
actual abstract class MultiPlatformTest {
  actual fun getTestSqlDriverFactory() = SqlDriverFactoryProvider(RuntimeEnvironment.getApplication()).getDriverFactory()
}
