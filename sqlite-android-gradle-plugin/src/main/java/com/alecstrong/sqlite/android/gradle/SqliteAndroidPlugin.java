package com.alecstrong.sqlite.android.gradle;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.plugins.JavaPluginConvention;

public class SqliteAndroidPlugin implements Plugin<Project> {
  private final FileResolver fileResolver;

  @Inject
  public SqliteAndroidPlugin(FileResolver fileResolver) {
    this.fileResolver = fileResolver;
  }

  @Override
  public void apply(Project project) {
    project.getConvention()
        .getPlugin(JavaPluginConvention.class)
        .getSourceSets()
        .all((sourceSet) -> {
          // Get .sqlite files.
          SourceDirectorySet sqliteSources =
              new DefaultSourceDirectorySet(((DefaultSourceSet) sourceSet).getDisplayName(),
                  fileResolver);
          sqliteSources.getFilter().include("**/*.sqlite");
          sqliteSources.srcDirs("src");

          String taskName = sourceSet.getTaskName("generate", "SQLiteInterface");
          SqliteAndroidTask task = project.getTasks().create(taskName, SqliteAndroidTask.class);
          task.setDescription(
              String.format("Generate Android interfaces for working with %s sqlite tables",
                  sourceSet.getName()));
          task.setSource(sqliteSources);

          project.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(taskName);
        });
  }
}
