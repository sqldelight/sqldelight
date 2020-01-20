plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
}

dependencies {
  compile(gradleApi())
  compile("org.jetbrains.kotlin:kotlin-stdlib:1.3.61")
}
