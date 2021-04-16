# Copyright 2020 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@rules_pkg//:pkg.bzl", "pkg_tar")

def release_archive(name, srcs = None, src_map = {}, package_dir = None, extension = "tgz", deps = None):
    """
    Creates an tar of the srcs, and renamed label artifacts.
    Usage:
    //:BUILD
    load("//kotlin/internal/utils:packager.bzl", "release_archive")
    release_archive(
        name = "release_archive",
        src_map = {
            "BUILD.release.bazel.bazel": "BUILD.bazel",
            "WORKSPACE.release.bazel": "WORKSPACE",
        },
        deps = [
            "//dep:pkg"
        ],
    )
    //dep:BUILD
    load("//kotlin/internal/utils:packager.bzl", "release_archive")
    release_archive(
        name = "pkg",
        srcs = [
            ":label_of_artifact",
        ],
    )
    Args:
        name: target identifier, points to a pkg_tar target.
        package_dir: directory to place the srcs, src_map, and dist_files under. Defaults to the current directory.
        dist_files: dict of <filename string>:<contents string> for files to be generated in the distribution artifact.
        src_map: dict of <label>:<name string> for labels to be renamed and included in the distribution.
        srcs: files to include in the distribution.
        ext: Extension of the archive. Controls the type of tar file generated.
        deps: release_archives to be included.
    """

    if package_dir == None:
        pkg_name = native.package_name()
        local_pkg_index = pkg_name.rfind("/")
        if local_pkg_index > -1:
            pkg_name = pkg_name[local_pkg_index:]
        package_dir = pkg_name

    if srcs == None:
        srcs = []

    for source, target in src_map.items():
        rename_name = name + "_" + target
        _rename(
            name = rename_name,
            source = source,
            target = target,
        )
        srcs += [rename_name]

    pkg_tar(
        name = name,
        srcs = srcs if srcs != None else [],
        extension = extension,
        package_dir = package_dir,
        visibility = ["//visibility:public"],
        deps = deps if deps != None else [],
    )

def _rename_impl(ctx):
    out_file = ctx.actions.declare_file(ctx.label.name + "/" + ctx.attr.target)
    in_file = ctx.file.source
    ctx.actions.run_shell(
        inputs = [in_file],
        outputs = [out_file],
        progress_message = "%s -> %s" % (in_file, ctx.attr.target),
        command = "mkdir -p {dir} && cp {in_file} {out_file}".format(
            dir = ctx.label.name,
            in_file = in_file.path,
            out_file = out_file.path,
        ),
    )
    return [DefaultInfo(files = depset([out_file]))]

_rename = rule(
    implementation = _rename_impl,
    attrs = {
        "source": attr.label(allow_single_file = True, mandatory = True),
        "target": attr.string(mandatory = True),
    },
)
