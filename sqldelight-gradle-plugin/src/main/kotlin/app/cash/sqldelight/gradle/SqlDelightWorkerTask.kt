package app.cash.sqldelight.gradle

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceTask
import org.gradle.process.JavaForkOptions
import org.gradle.workers.ClassLoaderWorkerSpec
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

/**
 * Common API for interacting with gradle workers
 * in tasks
 */
@CacheableTask
abstract class SqlDelightWorkerTask : SourceTask() {

  @get:Inject
  internal abstract val workerExecutor: WorkerExecutor

  /** @see ClassLoaderWorkerSpec.getClasspath */
  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

  /** @see JavaForkOptions.getEnvironment */
  @get:Input
  @get:Optional
  val environment: MapProperty<String, Any> =
    project.objects.mapProperty(String::class.java, Any::class.java)
      .convention(getInheritableEnvironmentVariables(System.getenv()))

  fun environment(name: String, value: Any): SqlDelightWorkerTask {
    environment.put(name, value)
    return this
  }

  fun environment(environmentVariables: Map<String, *>): SqlDelightWorkerTask {
    environment.putAll(environmentVariables)
    return this
  }

  /** @see JavaForkOptions.getSystemProperties */
  @get:Input
  @get:Optional
  val systemProperties: MapProperty<String, Any> =
    project.objects.mapProperty(String::class.java, Any::class.java)

  /** @see JavaForkOptions.getMinHeapSize */
  @get:Input
  @get:Optional
  val minHeapSize: Property<String> =
    project.objects.property(String::class.java)

  /** @see JavaForkOptions.getMaxHeapSize */
  @get:Input
  @get:Optional
  val maxHeapSize: Property<String> =
    project.objects.property(String::class.java).convention("512M")

  /** @see JavaForkOptions.jvmArgs */
  @get:Input
  @get:Optional
  val jvmArgs: ListProperty<String> =
    project.objects.listProperty(String::class.java)

  fun jvmArgs(arguments: Iterable<*>): SqlDelightWorkerTask {
    jvmArgs.addAll(arguments.map { it.toString() })
    return this
  }

  fun jvmArgs(vararg arguments: Any): SqlDelightWorkerTask {
    this.jvmArgs(listOf(*arguments))
    return this
  }

  internal fun workQueue(): WorkQueue =
    workerExecutor.processIsolation { workerSpec ->
      workerSpec.classpath.from(classpath)

      workerSpec.forkOptions { forkOptions ->
        forkOptions.defaultCharacterEncoding = "UTF-8"
        forkOptions.environment(environment.orNull)
        forkOptions.systemProperties(systemProperties.orNull)
        forkOptions.minHeapSize = minHeapSize.orNull
        forkOptions.maxHeapSize = maxHeapSize.orNull
        forkOptions.jvmArgs = jvmArgs.orNull
      }
    }

  /** Copied from Gradle's internal org.gradle.internal.jvm.Jvm.getInheritableEnvironmentVariables() */
  private fun getInheritableEnvironmentVariables(envVars: Map<String, Any>): Map<String, Any> {
    val appNameRegex = "APP_NAME_\\d+".toRegex()
    val javaMainClassRegex = "JAVA_MAIN_CLASS_\\d+".toRegex()

    val vars: MutableMap<String, Any> = HashMap()
    for ((key, value) in envVars) {
      // The following are known variables that can change between builds and should not be inherited
      if (appNameRegex.matches(key)
        || javaMainClassRegex.matches(key)
        || key == "TERM_SESSION_ID"
        || key == "ITERM_SESSION_ID") {
        continue
      }
      vars[key] = value
    }
    return vars
  }
}
