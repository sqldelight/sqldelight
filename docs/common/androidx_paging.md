# AndroidX Paging

To use SQLDelight with [Android's Paging 3 Library](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) add a dependency on the paging extension artifact.

=== "Kotlin"
    ```kotlin
    dependencies {
      implementation("app.cash.sqldelight:androidx-paging3-extensions:{{ versions.sqldelight }}")
    }
    ```
=== "Groovy"
    ```groovy
    dependencies {
      implementation "app.cash.sqldelight:androidx-paging3-extensions:{{ versions.sqldelight }}"
    }
    ```

{% include 'common/androidx_paging_usage.md' %}
