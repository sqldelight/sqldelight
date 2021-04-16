workspace(name = "rules_sqldelight")

KOTLIN_VERSION = "1.4.32"

KOTLINC_RELEASE_SHA = "dfef23bb86bd5f36166d4ec1267c8de53b3827c446d54e82322c6b6daad3594c"

KOTLINC_RELEASE_URL = "https://github.com/JetBrains/kotlin/releases/download/v{v}/kotlin-compiler-{v}.zip".format(v = KOTLIN_VERSION)

MAVEN_REPOSITORY_RULES_VERSION = "2.0.0-alpha-4"

MAVEN_REPOSITORY_RULES_SHA = "a6484fec8d1aebd4affff7ae1ee9b59141858b2c636222bdb619526ccd8b3358"

RULES_KOTLIN_VERSION = "1.5.0-alpha-3"

RULES_KOTLIN_SHA = "eeae65f973b70896e474c57aa7681e444d7a5446d9ec0a59bb88c59fc263ff62"

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

maybe(
    http_archive,
    name = "io_bazel_rules_kotlin",
    sha256 = RULES_KOTLIN_SHA,
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin_release.tgz" % RULES_KOTLIN_VERSION],
)

maybe(
    http_archive,
    name = "maven_repository_rules",
    sha256 = MAVEN_REPOSITORY_RULES_SHA,
    strip_prefix = "bazel_maven_repository-%s" % MAVEN_REPOSITORY_RULES_VERSION,
    type = "zip",
    urls = ["https://github.com/square/bazel_maven_repository/archive/%s.zip" % MAVEN_REPOSITORY_RULES_VERSION],
)

maybe(
    http_archive,
    name = "build_bazel_rules_android",
    sha256 = "cd06d15dd8bb59926e4d65f9003bfc20f9da4b2519985c27e190cddc8b7a7806",
    strip_prefix = "rules_android-0.1.1",
    urls = ["https://github.com/bazelbuild/rules_android/archive/v0.1.1.zip"],
)

load("@io_bazel_rules_kotlin//kotlin:dependencies.bzl", "kt_download_local_dev_dependencies")

kt_download_local_dev_dependencies()

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kotlin_repositories(compiler_release = {
    "urls": [KOTLINC_RELEASE_URL],
    "sha256": KOTLINC_RELEASE_SHA,
})

register_toolchains("//:kotlin_toolchain")

load("@maven_repository_rules//maven:maven.bzl", "maven_repository_specification")

maven_repository_specification(
    name = "maven_sqldelight",
    artifacts = {
        "androidx.annotation:annotation:1.0.0": {"insecure": True},
        "androidx.collection:collection-ktx:1.1.0": {"insecure": True},
        "androidx.collection:collection:1.1.0": {"insecure": True},
        "androidx.sqlite:sqlite-framework:2.1.0": {"insecure": True},
        "androidx.sqlite:sqlite-ktx:2.0.1": {"insecure": True},
        "androidx.sqlite:sqlite:2.0.1": {"insecure": True},
        "com.alecstrong:sqlite-psi-core:0.3.4": {"insecure": True},
        "com.annimon:stream:1.1.7": {"insecure": True},
        "com.beust:jcommander:1.78": {"insecure": True},
        "com.squareup.moshi:moshi:1.9.2": {"insecure": True},
        "com.squareup.okio:okio:1.16.0": {"insecure": True},
        "com.squareup.sqldelight:android-driver:1.4.0": {"insecure": True},
        "com.squareup.sqldelight:core:1.4.0": {"insecure": True},
        "com.squareup.sqldelight:migrations:1.4.0": {"insecure": True},
        "com.squareup.sqldelight:runtime-jvm:1.4.0": {"insecure": True},
        "com.squareup:javapoet:1.10.0": {"insecure": True},
        "com.squareup:kotlinpoet:1.5.0": {"insecure": True},
        "de.danielbechler:java-object-diff:0.95": {"insecure": True},
        "org.antlr:antlr4-runtime:4.5.3": {"insecure": True},
        "org.jetbrains.kotlin:kotlin-reflect:%s" % KOTLIN_VERSION: {"insecure": True},
        "org.jetbrains.kotlin:kotlin-stdlib-common:%s" % KOTLIN_VERSION: {"insecure": True},
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:%s" % KOTLIN_VERSION: {"insecure": True},
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:%s" % KOTLIN_VERSION: {"insecure": True},
        "org.jetbrains.kotlin:kotlin-stdlib:%s" % KOTLIN_VERSION: {"insecure": True},
        "org.jetbrains:annotations:13.0": {"insecure": True},
        "org.ow2.asm:asm-analysis:7.0": {"insecure": True},
        "org.ow2.asm:asm-commons:7.0": {"insecure": True},
        "org.ow2.asm:asm-tree:7.0": {"insecure": True},
        "org.ow2.asm:asm:7.0": {"insecure": True},
        "org.pantsbuild:jarjar:1.7.2": {
            "insecure": True,
            "exclude": [
                "org.apache.ant:ant",
                "org.apache.maven:maven-plugin-api",
            ],
        },
        "org.slf4j:slf4j-api:1.7.22": {"insecure": True},
        "org.threeten:threetenbp:1.3.3": {"insecure": True},
        "org.xerial:sqlite-jdbc:3.21.0.1": {"insecure": True},
        "us.fatehi:schemacrawler-api:14.16.04.01-java7": {"insecure": True},
        "us.fatehi:schemacrawler-sqlite:14.16.04.01-java7": {"insecure": True},
        "us.fatehi:schemacrawler-tools:14.16.04.01-java7": {"insecure": True},
    },
    fetch_threads = 20,
    repository_urls = {
        "central": "https://repo1.maven.org/maven2",
        "android": "https://maven.google.com",
    },
)

load(":intellij.bzl", "intellij_core_repository")

intellij_core_repository(
    name = "intellij",
    sha256 = "2500339706e2951ae63a3c6e82e31a1da26adeee41a79724079420c2f29e18bb",
    version = "201.8743.12",
)

android_sdk_repository(
    name = "androidsdk",
    api_level = 29,
    build_tools_version = "29.0.3",
)
