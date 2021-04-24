# Contributing

If you would like to contribute code to this project you can do so through GitHub by
forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions
and style in order to keep the code as readable as possible.

Before your code can be accepted into the project you must also sign the
[Individual Contributor License Agreement (CLA)][1].


 [1]: https://spreadsheets.google.com/spreadsheet/viewform?formkey=dDViT2xzUHAwRkI3X3k5Z0lQM091OGc6MQ&ndplr=1
 
## SQLDelight 

If you're looking to get started with contributing, look below for specific guides depending on which part
of SQLDelight you'd like to contribute to. If you're still unsure, comment in the issue you're looking in to
with where you're getting stuck and we'll respond there - or create an issue for the thing you're trying to do
and start the discussion.

### IDE Plugin

If you want to fix a bug or extend the IDE, code changes will likely happen in the `sqldelight-idea-plugin` module.
You can test your changes using the `./gradlew runIde` task and you can live debug using `./gradlew runIde --debug-jvm`.

If you're encountering a bug in the IDE but cannot reproduce it in a sample project, you can live debug your IDE. You'll
need a second installation of IntelliJ to do this. You can use [Toolbox](https://www.jetbrains.com/toolbox-app/) to do
this by scrolling to the bottom of the IDE list and selecting a different version of IntelliJ.

In the IDE you'd like to use the debugger in, check out the SQLDelight repo and then create a new `Remote` Run Configuration.
It will already populate "Command line arguments for remote JVM", something like `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`.
Copy that value, then open the IDE you would like to debug. Select `Help -> Edit Custom VM Options`, and paste the line you copied to the bottom
of the file that is opened. Restart the IDE you want to debug, then once it's started up open the IDE you created the configuration in, and attach
the debugger using the remote configuration you created.

For more information on building IDE plugins and features for them see the [Official Jetbrains Documentation](https://jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support.html)
or join the [Jetbrains Platform Slack](https://blog.jetbrains.com/platform/2019/10/introducing-jetbrains-platform-slack-for-plugin-developers/).

### Drivers

If you're interested in creating your own driver, you can do so outside of the SQLDelight repository using the `runtime` artifact. To test the driver
you can depend on the `driver-test` and extend `DriverTest` and `TransactionTest` to ensure it works as SQLDelight would expect.

### Gradle

If you're encountering a gradle issue, start by creating a test fixture in `sqldelight-gradle-plugin/src/test` similar to the other folders there
which reproduces your issue. Feel free to just open a PR with this failing test if you don't know how to fix! Test cases are greatly appreciated.
The integration tests show how to set up an entire gradle project which will run SQLite/MySQL/PostgreSQL/etc and execute SQL queries using their
respective runtime environments and SQLDelight. Consider adding a test to these already existing integration tests if you're encountering
runtime issues in SQLDelight.

### Compiler

There are many layers to SQLDelight's compiler - if you are strictly interested in the codegen (and not the parsing of SQL) then you will want to
make your contributions in the `sqldelight-compiler` module. If you are interested in the parser you'll need to contribute to
[sql-psi](https://github.com/alecstrong/sql-psi). SQLDelight uses [kotlinpoet](https://github.com/square/kotlinpoet) for generating
kotlin code, be sure to use it's APIs for referencing kotlin types so imports still work correctly. If you modify the codegen in any way,
run a `./gradlew build` before opening a pull request, as it will update the integration test in `sqldelight-compiler:integration-tests`. If you'd
like to write an integration test (meaning running SQL queries in a runtime environment), add a test to `sqldelight-compiler:integration-tests`.

---

## SQL PSI

In the next section we will go through how to contribute to the parser and PSI layer, but before doing that you should read
a blog post on [multiple dialects](https://www.alecstrong.com/posts/multiple-dialects/) to understand the various moving pieces in [sql-psi](https://github.com/AlecStrong/sql-psi).
As with SQLDelight, if you're encountering an issue but don't know how to contribute a fix or need assistance, comment in the GitHub issue or
create a new one to start the discussion.

For any changes in SQL-PSI, you will want to add a test fixture in the corresponding `core/src/test/fixtures_*` folder. The `fixtures` folder (no suffix)
runs for all dialects. After your change has been merged to sql-psi, if there are changes you also need to make in SQLDelight, check out the 
`sql-psi-dev` branch on SQLDelight and target it with your PR. It uses the snapshot releases of sql-psi so you can build your SQLDelight change
roughly 10 minutes after the sql-psi change has been merged.

### Grammar

If you are adding to the grammar, first decide if this is a new rule you are adding to an existing grammar, or a rule you would like to override
from ANSI SQL (which is found in [sql.bnf](https://github.com/AlecStrong/sql-psi/blob/master/core/src/main/kotlin/com/alecstrong/sql/psi/core/sql.bnf)).
In both cases, you will want to define that rule in your new grammar, but in the case of overriding an ANSI SQL rule, add it to the overrides list and
set the override attribute on the rule:

```bnf
overrides ::= my_rule

my_rule ::= SOME_TOKEN {
  override = true
}
```

The definition of your rule should start by being an exact copy/paste of the rule from ANSI-SQL. To reference rules from ANSI-SQL, you
need to surround it in {}, so you should surround all external rules in your overriding rule with {}:

```bnf
my_rule ::= internal_rule {external_rule} {
  override = true
}
internal_rule ::= SOME_TOKEN
```

One caveat is that referencing the `expr` rule from ANSI-SQL should look like `<<expr '-1'>>` because it is special and cannot be overridden.

Any tokens that you want to use from ANSI SQL should also be manually imported:

```bnf
{
  parserImports = [
    "static com.alecstrong.sql.psi.core.psi.SqlTypes.DELETE"
    "static com.alecstrong.sql.psi.core.psi.SqlTypes.FROM"
  ]
}
overrides ::= delete

delete ::= DELETE FROM {table_name} {
  override = true
}
```

Dialects cannot add their own tokens, but you can require exact text by surrounding it with "":

```bnf
my_rule ::= "SOME_TOKEN"
```

Overriding rules must still generate code which confirms to the original rules types, so make sure to
`implement` and `extend` the existing types for the original rule:

```bnf
my_rule ::= internal_rule {external_rule} {
  extends = "com.alecstrong.sql.psi.core.psi.impl.SqlMyRuleImpl"
  implements = "com.alecstrong.sql.psi.core.psi.SqlMyRule"
  overrides = true
}
```

To see an example of overriding rules in the grammar, check out [this pr](https://github.com/AlecStrong/sql-psi/pull/163/files)
which adds `RETURNING` syntax to PostgreSQL.

### Rule Behavior

Often times you want to modify the behavior of the PSI layer (for example throwing errors for situations you want to fail
compilation for). To do this, have your rule use a `mixin` instead of an `extends` which is a class you write containing that new logic:

```bnf
my_rule ::= interal_rule {external_rule} {
  mixin = "com.alecstrong.sql.psi.MyRuleMixin"
  implements = "com.alecstrong.sql.psi.core.psi.SqlMyRule"
  overrides = true
}
```

And then in that class ensure that it implements the original ANSI SQL type and the SQL-PSI base class `SqlCompositeElementImpl`:

```
class MyRule(
  node: ASTNode
) : SqlCompositeElementImpl(node),
    SqlMyRule {
  fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (internal_rule.text == "bad_text") {
      annotationHolder.createErrorAnnotation("Invalid text value", internal_rule)
    }
  }
}
```

For example, the [DropIndexMixin](https://github.com/AlecStrong/sql-psi/blob/f1137ff82dd0aa77f741a09d88855fbf9b751c00/core/src/main/kotlin/com/alecstrong/sql/psi/core/psi/mixins/DropIndexMixin.kt)
verifies the index being dropped exists in the schema.

---

If you have a question about contributing not covered in this doc please feel free to open an issue on SqlDelight or open a PR so we can
work on improving it!