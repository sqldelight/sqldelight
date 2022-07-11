package app.cash.sqldelight.gradle.squash

import app.cash.sqldelight.VERSION
import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.psi.SqlDelightImportStmtList
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightWorkerTask
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.psi.PsiFileFactory
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.util.ServiceLoader

@CacheableTask
abstract class MigrationSquashTask : SqlDelightWorkerTask() {
  @Suppress("unused")
  // Required to invalidate the task on version updates.
  @Input
  val pluginVersion = VERSION

  @Input val projectName: Property<String> = project.objects.property(String::class.java)

  @Nested lateinit var properties: SqlDelightDatabasePropertiesImpl

  @Nested lateinit var compilationUnit: SqlDelightCompilationUnitImpl

  @TaskAction
  fun generateSquashedMigrationFile() {
    workQueue().submit(GenerateMigration::class.java) {
      it.moduleName.set(projectName)
      it.properties.set(properties)
      it.compilationUnit.set(compilationUnit)
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface GenerateSchemaWorkParameters : WorkParameters {
    val moduleName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
    val compilationUnit: Property<SqlDelightCompilationUnit>
  }

  abstract class GenerateMigration : WorkAction<GenerateSchemaWorkParameters> {

    private val sourceFolders: List<File>
      get() = parameters.compilationUnit.get().sourceFolders.map { it.folder }

    override fun execute() {
      val properties = parameters.properties.get()
      val environment = SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = emptyList(),
        moduleName = parameters.moduleName.get(),
        properties = properties,
        verifyMigrations = false,
        compilationUnit = parameters.compilationUnit.get(),
        dialect = ServiceLoader.load(SqlDelightDialect::class.java).single(),
      )

      val fileFromText = { text: String ->
        PsiFileFactory.getInstance(environment.project).createFileFromText(MigrationLanguage, text) as SqlFileBase
      }

      val ansiSquasher = AnsiSqlMigrationSquasher(fileFromText)
      val squasher = environment.dialect.migrationSquasher(ansiSquasher)
      ansiSquasher.squasher = squasher // For recursion to ensure the dialect squasher gets called.

      val imports = linkedSetOf<String>()

      var newMigrations = fileFromText("")

      // Generate the new files.
      var topVersion = 0
      lateinit var migrationDirectory: File
      environment.forMigrationFiles { migrationFile ->
        if (migrationFile.version > topVersion) {
          topVersion = migrationFile.version
          migrationDirectory = File(migrationFile.virtualFile!!.parent.path)
        }

        val migrations = migrationFile.sqlStmtList?.children
          ?.filterIsInstance<SqlDelightImportStmtList>()?.single()
        migrations?.importStmtList?.forEach {
          imports.add(it.javaType.text)
        }

        migrationFile.sqlStmtList?.stmtList?.forEach {
          newMigrations = fileFromText(squasher.squish(it, into = newMigrations))
        }
      }

      var text = newMigrations.text
      if (imports.isNotEmpty()) {
        text = """
          |${imports.joinToString(separator = "\n", transform = { "import $it;" })}
          |
          |$text
        """.trimMargin()
      }

      File(migrationDirectory, "_$topVersion.sqm").writeText(text)
    }
  }
}
