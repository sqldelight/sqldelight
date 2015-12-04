package com.alecstrong.sqlite.android;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SqliteAndroidPluginTest {
  @Rule public TemporaryFixture fixture = new TemporaryFixture(false);

  private final GradleRunner gradleRunner = GradleRunner.create();

  private List<File> pluginClasspath;

  @Before public void setup() throws IOException {
    URL pluginClasspathResource = getClass().getClassLoader().getResource("plugin-classpath.txt");
    if (pluginClasspathResource == null) {
      throw new IllegalStateException(
          "Did not find plugin classpath resource, run `testClasses` build task.");
    }

    pluginClasspath = Lists.transform(Resources.readLines(pluginClasspathResource, UTF_8),
        File::new);

    File localProperties = new File(fixture.getRoot(), "local.properties");
    Files.write("sdk.dir=/Users/astrong/Library/Android/sdk", localProperties, UTF_8);
  }

  @FixtureName("works-fine")
  @Test public void worksFine() {
    BuildResult result = gradleRunner.withProjectDir(fixture.getRoot())
        .withArguments("assembleDebug", "--stacktrace")
        .withPluginClasspath(pluginClasspath)
        .build();

    assertThat(result.getStandardOutput()).contains("BUILD SUCCESSFUL");
  }

  @FixtureName("works-fine-as-library")
  @Test public void worksFineAsLibrary() {
    BuildResult result = gradleRunner.withProjectDir(fixture.getRoot())
        .withArguments("assembleDebug", "--stacktrace")
        .withPluginClasspath(pluginClasspath)
        .build();

    assertThat(result.getStandardOutput()).contains("BUILD SUCCESSFUL");
  }

  @FixtureName("unknown-class-type")
  @Test public void unknownClassType() {
    BuildResult result = prepareTask().buildAndFail();

    assertThat(result.getStandardError()).contains(
        "Table.sq line 9:2 - Couldnt make a guess for type of colum a_class\n"
            + "  07\t\tCREATE TABLE test (\n"
            + "  08\t\t  id INT PRIMARY KEY NOT NULL,\n"
            + "  09\t\t  a_class CLASS('')\n"
            + "  \t\t    ^^^^^^^^^^^^^^^^^\n"
            + "  10\t\t)");
  }

  @FixtureName("missing-package-statement")
  @Test public void missingPackageStatement() {
    BuildResult result = prepareTask().buildAndFail();

    assertThat(result.getStandardError()).contains(
        "Table.sq line 1:0 - mismatched input 'CREATE' expecting {<EOF>, K_PACKAGE, UNEXPECTED_CHAR}");
  }

  @FixtureName("syntax-error")
  @Test public void syntaxError() {
    BuildResult result = prepareTask().buildAndFail();

    assertThat(result.getStandardError()).contains(
        "Table.sq line 5:4 - mismatched input 'FRM' expecting {';', ',', K_EXCEPT, K_FROM, K_GROUP, K_INTERSECT, K_LIMIT, K_ORDER, K_UNION, K_WHERE}");
  }

  @FixtureName("unknown-type")
  @Test public void unknownType() {
    BuildResult result = prepareTask().buildAndFail();

    assertThat(result.getStandardError()).contains(
        "Table.sq line 5:15 - no viable alternative at input 'LIST'");
  }

  private GradleRunner prepareTask() {
    return gradleRunner.withProjectDir(fixture.getRoot())
        .withArguments("generateSqliteInterface", "--stacktrace")
        .withPluginClasspath(pluginClasspath);
  }
}
