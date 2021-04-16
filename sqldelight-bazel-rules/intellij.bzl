_URL = "https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/{version}/ideaIU-{version}.zip"

_BUILD_FILE = """
load("@rules_java//java:defs.bzl", "java_import")

java_import(
    name = "idea",
    jars = [
        "lib/extensions.jar",
        "lib/jdom.jar",
        "lib/guava-28.2-jre.jar",
        "lib/platform-api.jar",
        "lib/platform-impl.jar",
        "lib/openapi.jar",
        "lib/testFramework.jar",
        "lib/trove4j.jar",
        "lib/util.jar",
    ],
    visibility = ["//visibility:public"],
)
"""

def _intellij_respository_impl(repo_ctx):
    repo_ctx.download_and_extract(
        url = _URL.format(version = repo_ctx.attr.version),
        sha256 = repo_ctx.attr.sha256,
        type = "zip",
        output = ".",
    )

    repo_ctx.file("BUILD.bazel", _BUILD_FILE)

intellij_core_repository = repository_rule(
    implementation = _intellij_respository_impl,
    attrs = {
        "version": attr.string(mandatory = True, doc = "Intellij version."),
        "sha256": attr.string(mandatory = True, doc = "Intellij arcive sha"),
    },
)
