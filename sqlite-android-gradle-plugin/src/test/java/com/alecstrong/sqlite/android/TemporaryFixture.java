package com.alecstrong.sqlite.android;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TemporaryFixture extends TemporaryFolder {
  private String fixtureName;
  private final boolean deleteAfter;

  public TemporaryFixture() {
    this(true);
  }

  public TemporaryFixture(boolean deleteAfter) {
    this.deleteAfter = deleteAfter;
  }

  @Override protected void before() throws Throwable {
    super.before();

    File fixtures = new File("src/test/fixtures");
    File from = new File(fixtures, fixtureName);

    File root = getRoot();
    FileUtils.copyDirectory(from, root);
  }

  @Override public void delete() {
    if (deleteAfter) {
      super.delete();
    }
  }

  @Override public Statement apply(Statement base, Description description) {
    FixtureName annotation = description.getAnnotation(FixtureName.class);
    if (annotation == null) {
      throw new IllegalStateException(String.format("Test '%s' missing @FixtureName annotation.",
          description.getDisplayName()));
    }
    fixtureName = annotation.value();

    return super.apply(base, description);
  }
}
