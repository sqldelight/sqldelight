package app.cash.sqldelight.core.lang

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MigrationFileTest {
  @Test
  fun `parse migration file name with only digits`() {
    assertThat("1.sqm".toMigrationVersion()).isEqualTo(1L)
  }

  @Test
  fun `parse migration file name with leading zeroes`() {
    assertThat("001.sqm".toMigrationVersion()).isEqualTo(1L)
  }

  @Test
  fun `parse migration file name with spaces`() {
    assertThat("1 2 3.sqm".toMigrationVersion()).isEqualTo(123L)
  }

  @Test
  fun `parse migration file name with dashes`() {
    assertThat("1-23-87.sqm".toMigrationVersion()).isEqualTo(12387L)
  }

  @Test
  fun `parse migration file name with additional suffix`() {
    assertThat("001_add_foo_table.sqm".toMigrationVersion()).isEqualTo(1L)
  }

  @Test
  fun `parse migration file name with additional suffix containing numbers`() {
    assertThat("001_add_foo_table2.sqm".toMigrationVersion()).isEqualTo(1L)
  }

  @Test
  fun `parse migration file name with additional prefix`() {
    assertThat("v1.sqm".toMigrationVersion()).isEqualTo(1L)
  }

  @Test
  fun `parse migration file name with a larger than Int value`() {
    assertThat("1_000_000_000_000.sqm".toMigrationVersion()).isEqualTo(1_000_000_000_000L)
  }

  @Test
  fun `parse migration file name with an iso timestamp`() {
    assertThat("2025-05-13T08:18:57.418Z_foo_bar.sqm".toMigrationVersion()).isEqualTo(2025_05_13_08_18_57_418L)
  }

  @Test
  fun `parse migration file name with no numbers`() {
    assertThat("foo_bar.sqm".toMigrationVersion()).isEqualTo(0L)
  }
}
