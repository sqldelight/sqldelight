package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.google.common.base.CaseFormat;
import java.util.Arrays;
import javax.inject.Inject;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.compile.JavaCompile;

public class SqliteAndroidPlugin implements Plugin<Project> {
  private final FileResolver fileResolver;

  @Inject
  public SqliteAndroidPlugin(FileResolver fileResolver) {
    this.fileResolver = fileResolver;
  }

  @Override
  public void apply(Project project) {
    Task generateSqlite = project.task("generateSqliteInterface");
    project.afterEvaluate(afterEvaluateProject -> {
      ((DependencyHandler) project.property("dependencies"))
          .add("compile", "com.android.support:support-annotations:23.1.1");
      DomainObjectSet<? extends BaseVariant> variants;
      if (!project.hasProperty("android")) {
        throw new IllegalStateException("Must have applied the android plugin");
      }
      if (project.property("android") instanceof AppExtension) {
        variants = ((AppExtension) project.property("android")).getApplicationVariants();
      } else {
        variants = ((LibraryExtension) project.property("android")).getLibraryVariants();
      }
      variants.all(variant -> {
        // Get .sqlite files.
        SourceDirectorySet sqliteSources =
            new DefaultSourceDirectorySet(variant.getName(), fileResolver);
        sqliteSources.getFilter().include("**/*." + SqliteCompiler.getFileExtension());
        sqliteSources.srcDirs("src");

        // Set up the generateSql task.
        String taskName = "generate"
            + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, variant.getName())
            + "SqliteInterface";
        SqliteAndroidTask task = project.getTasks().create(taskName, SqliteAndroidTask.class);
        task.setGroup("sqlite");
        task.setBuildDirectory(project.getBuildDir());
        task.setDescription(String.format(
            "Generate Android interfaces for working with %s sqlite tables",
            variant.getName()));
        task.setSource(sqliteSources);

        generateSqlite.dependsOn(task);

        // Update the variant to include the sqlite task.
        variant.registerJavaGeneratingTask(task, task.getOutputDirectory());
        variant.addJavaSourceFoldersToModel(task.getOutputDirectory());
        ((JavaCompile) variant.getJavaCompiler()).getOptions().getCompilerArgs().addAll(
            Arrays.asList("-sourcepath", String.valueOf(task.getOutputDirectory())));
      });
    });
  }
}
