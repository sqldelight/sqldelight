"""provides an sqldelight compiler"""

def _sqldelight_codegen_impl(ctx):
    srcjar = ctx.outputs.srcjar

    args = ctx.actions.args()
    args.add("-o", srcjar)

    if not ctx.attr.module_name or not ctx.attr.package_name:
        fail("Non-legacy SQLDelightc requires both module_name and package_name set.")
    args.add("--module_name", ctx.attr.module_name)
    args.add("--package_name", ctx.attr.package_name)
    args.add_all(ctx.files.srcs)
    src_roots = {}
    for f in ctx.files.srcs:
        (pre, src, rel_name) = f.short_path.partition(ctx.attr.src_dir)
        src_roots[pre + src] = True

    args.add_joined("--src_dirs", src_roots.keys(), join_with = ",")

    ctx.actions.run(
        executable = ctx.executable._sqldelight_compiler,
        inputs = ctx.files.srcs,
        outputs = [srcjar],
        arguments = [args],
    )
    return struct(
        providers = [DefaultInfo(files = depset([srcjar]))],
    )

sqldelight_codegen = rule(
    _sqldelight_codegen_impl,
    attrs = {
        "_sqldelight_compiler": attr.label(
            default = Label("@rules_sqldelight//:sqldelightc"),
            executable = True,
            cfg = "host",
        ),
        "srcs": attr.label_list(allow_files = [".sq"]),
        "src_dir": attr.string(
            mandatory = True,
            doc = "root directory of the source tree, used to derived the classnames.",
        ),
        "module_name": attr.string(),
        "package_name": attr.string(),
    },
    output_to_genfiles = True,
    outputs = {
        "srcjar": "%{name}_sqldelight.srcjar",
    },
)
