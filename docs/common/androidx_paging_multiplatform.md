# AndroidX Paging

To use SQLDelight with [Android's Paging 3 Library](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) add a dependency on the paging extension artifact.
Multiplatform support for AndroidX Paging is provided via [Multiplatform Paging](https://github.com/cashapp/multiplatform-paging).

=== "Kotlin"
    ```kotlin
    kotlin {
      sourceSets.commonMain.dependencies {
        implementation("app.cash.sqldelight:androidx-paging3-extensions:{{ versions.sqldelight }}")
      }
    }
    ```
=== "Groovy"
    ```groovy
    kotlin {
      sourceSets.commonMain.dependencies {
        implementation "app.cash.sqldelight:androidx-paging3-extensions:{{ versions.sqldelight }}"
      }
    }
    ```

{% include 'common/androidx_paging_usage.md' %}
