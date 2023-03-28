import app.sqltest.shared.common.data.SqlDriverFactoryProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
actual abstract class MultiPlatformTest {
    actual fun getTestSqlDriverFactory() = SqlDriverFactoryProvider(RuntimeEnvironment.getApplication()).getDriverFactory()
}
