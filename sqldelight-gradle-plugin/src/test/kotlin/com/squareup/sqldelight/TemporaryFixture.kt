package com.squareup.sqldelight

import java.io.File
import org.apache.commons.io.FileUtils
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TemporaryFixture constructor(private val deleteAfter: Boolean = true) : TemporaryFolder() {
  private var fixtureName: String? = null

  override fun before() {
    super.before()

    val fixtures = File("src/test/fixtures")
    val from = File(fixtures, fixtureName)

    val root = root
    FileUtils.copyDirectory(from, root)
  }

  override fun delete() {
    if (deleteAfter) {
      super.delete()
    }
  }

  override fun apply(base: Statement, description: Description): Statement {
    val annotation = description.getAnnotation(FixtureName::class.java) ?:
        throw IllegalStateException(
            "Test '%s' missing @FixtureName annotation.".format(description.displayName))
    fixtureName = annotation.value

    return super.apply(base, description)
  }
}
