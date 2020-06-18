First apply the gradle plugin in your project.

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:{{ versions.sqldelight }}'
  }
}

apply plugin: 'com.squareup.sqldelight'

sqldelight {
  Database { // This will be the name of the generated database class.
    packageName = "com.example"