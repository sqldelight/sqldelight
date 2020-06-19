## Flow

To consume a query as a Flow, depend on the Coroutines extensions artifact and use the extension method it provides:

```groovy
kotlin {
  sourceSets.commonMain.dependencies {
    implementation "com.squareup.sqldelight:coroutines-extensions:{{ versions.sqldelight }}"
  }
}
```

{% include 'common/coroutines-usage.md' %}
