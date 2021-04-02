First apply the gradle plugin in your project.

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
    maven { url 'https://www.jetbrains.com/intellij-repository/releases' }
    maven { url "https://jetbrains.bintray.com/intellij-third-party-dependencies" }
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:{{ versions.sqldelight }}'
  }
}

apply plugin: 'com.squareup.sqldelight'

sqldelight {
  Database { // This will be the name of the generated database class.
    packageName = "com.example"