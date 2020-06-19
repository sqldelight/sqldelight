## Flow

To consume a query as a Flow, depend on the Coroutines extensions artifact and use the extension method it provides:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:coroutines-extensions-jvm:{{ versions.sqldelight }}"
}
```

{% include 'common/coroutines-usage.md' %}
