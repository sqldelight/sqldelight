Change Log
==========

Version 1.1.4 *(2019-07-11)*
----------------------------

* New: [Runtime] Experimental kotlin Flow api.
* Fix: [Gradle] Kotlin/Native 1.3.40 compatibility.
* Fix: [Gradle] #1243 Fix for usage of SQLDelight with Gradle configure on demand.
* Fix: [Gradle] #1385 Fix for usage of SQLDelight with incremental annotation processing.
* Fix: [Gradle] Allow gradle tasks to cache.
* Fix: [Gradle] #1274 Enable usage of sqldelight extension with kotlin dsl.
* Fix: [Compiler] Unique ids are generated for each query deterministically.
* Fix: [Compiler] Only notify listening queries when a transaction is complete.
* Fix: [JVM Driver] #1370 Force JdbcSqliteDriver users to supply a DB URL

Version 1.1.3 *(2019-04-14)*
----------------------------

* Gradle Metadata 1.0 release.

Version 1.1.2 *(2019-04-14)*
----------------------------

* New: [Runtime] #1267 Logging driver decorator.
* Fix: [Compiler] #1254 Split string literals which are longer than 2^16 characters.
* Fix: [Gradle] #1260 generated sources are recognized as iOS source in Multiplatform Project.
* Fix: [IDE] #1290 kotlin.KotlinNullPointerException in CopyAsSqliteAction.kt:43.
* Fix: [Gradle] #1268 Running linkDebugFrameworkIos* tasks fail in recent versions.

Version 1.1.1 *(2019-03-01)*
----------------------------

* Fix: [Gradle] Fix module dependency compilation for android projects.
* Fix: [Gradle] #1246 Set up api dependencies in afterEvaluate.
* Fix: [Compiler] Array types are properly printed.

Version 1.1.0 *(2019-02-27)*
----------------------------

* New: [Gradle] #502 Allow specifying schema module dependencies.
* Enhancement: [Compiler] #1111 Table errors are sorted before other errors.
* Fix: [Compiler] #1225 Return the correct type for REAL literals.
* Fix: [Compiler] #1218 docid propagates through triggers.

Version 1.0.3 *(2019-01-30)*
----------------------------

* Enhancement: [Runtime] #1195 Native Driver/Runtime Arm32.
* Enhancement: [Runtime] #1190 Expose the mapper from the Query type.

Version 1.0.2 *(2019-01-26)*
----------------------------

* Fix: [Gradle Plugin] Update to kotlin 1.3.20.
* Fix: [Runtime] Transactions no longer swallow exceptions.

Version 1.0.1 *(2019-01-21)*
----------------------------

* Enhancement: [Native Driver] Allow passing directory name to DatabaseConfiguration.
* Enhancement: [Compiler] #1173 Files without a package fail compilation.
* Fix: [IDE] Properly report IDE errors to Square.
* Fix: [IDE] #1162 Types in the same package show as error but work fine.
* Fix: [IDE] #1166 Renaming a table fails with NPE.
* Fix: [Compiler] #1167 Throws an exception when trying to parse complex SQL statements with UNION and SELECT.

Version 1.0.0 *(2019-01-08)*
----------------------------

* New: Complete overhaul of generated code, now in kotlin.
* New: RxJava2 extensions artifact.
* New: Android Paging extensions artifact.
* New: Kotlin Multiplatform support.
* New: Android, iOS and JVM SQLite driver artifacts.
* New: Transaction API.

Version 0.7.0 *(2018-02-12)*
----------------------------

 * New: Generated code has been updated to use the Support SQLite library only. All queries now generate statement objects instead of a raw strings.
 * New: Statement folding in the IDE.
 * New: Boolean types are now automatically handled.
 * Fix: Remove deprecated marshals from code generation.
 * Fix: Correct 'avg' SQL function type mapping to be REAL.
 * Fix: Correctly detect 'julianday' SQL function.


Version 0.6.1 *(2017-03-22)*
----------------------------

 * New: Delete Update and Insert statements without arguments get compiled statements generated.
 * Fix: Using clause within a view used in a subquery doesn't error.
 * Fix: Duplicate types on generated Mapper removed.
 * Fix: Subqueries can be used in expressions that check against arguments.

Version 0.6.0 *(2017-03-06)*
----------------------------

 * New: Select queries are now exposed as a `SqlDelightStatement` factory instead of string constants.
 * New: Query JavaDoc is now copied to statement and mapper factories.
 * New: Emit string constants for view names.
 * Fix: Queries on views which require factories now correctly require those factories are arguments.
 * Fix: Validate the number of arguments to an insert matches the number of columns specified.
 * Fix: Properly encode blob literals used in where clauses.
 * Gradle 3.3 or newer is required for this release.

Version 0.5.1 *(2016-10-24)*
----------------------------

 * New: Compiled statements extend an abstract type.
 * Fix: Primitive types in parameters will be boxed if nullable.
 * Fix: All required factories for bind args are present in factory method.
 * Fix: Escaped column names are marshalled correctly.

Version 0.5.0 *(2016-10-19)*
----------------------------

 * New: SQLite arguments can be passed typesafely through the Factory
 * New: IntelliJ plugin performs formatting on .sq files
 * New: Support for SQLite timestamp literals
 * Fix: Parameterized types can be clicked through in IntelliJ
 * Fix: Escaped column names no longer throw RuntimeExceptions if grabbed from Cursor.
 * Fix: Gradle plugin doesn't crash trying to print exceptions.

Version 0.4.4 *(2016-07-20)*
----------------------------

 * New: Native support for shorts as column java type
 * New: Javadoc on generated mappers and factory methods
 * Fix: group_concat and nullif functions have proper nullability
 * Fix: Compatibility with Android Studio 2.2-alpha
 * Fix: WITH RECURSIVE no longer crashes plugin

Version 0.4.3 *(2016-07-07)*
----------------------------

 * New: Compilation errors link to source file.
 * New: Right-click to copy SQLDelight code as valid SQLite.
 * New: Javadoc on named statements will appear on generated Strings.
 * Fix: Generated view models include nullability annotations.
 * Fix: Generated code from unions has proper type and nullability to support all possible columns.
 * Fix: sum and round SQLite functions have proper type in generated code.
 * Fix: CAST's, inner selects bugfixes.
 * Fix: Autocomplete in CREATE TABLE statements.
 * Fix: SQLite keywords can be used in packages.

Version 0.4.2 *(2016-06-16)*
----------------------------

 * New: Marshal can be created from the factory.
 * Fix: IntelliJ plugin generates factory methods with proper generic order.
 * Fix: Function names can use any casing.

Version 0.4.1 *(2016-06-14)*
----------------------------

 * Fix: IntelliJ plugin generates classes with proper generic order.
 * Fix: Column definitions can use any casing.

Version 0.4.0 *(2016-06-14)*
----------------------------

 * New: Mappers are generated per query instead of per table.
 * New: Java types can be imported in .sq files.
 * New: SQLite functions are validated.
 * Fix: Remove duplicate errors.
 * Fix: Uppercase column names and java keyword column names do not error.

Version 0.3.2 *(2016-05-14)*
----------------------------

 * New: Autocompletion and find usages now work for views and aliases.
 * Fix: Compile-time validation now allows functions to be used in selects.
 * Fix: Support insert statements which only declare default values.
 * Fix: Plugin no longer crashes when a project not using SQLDelight is imported.


Version 0.3.1 *(2016-04-27)*
----------------------------

  * Fix: Interface visibility changed back to public to avoid Illegal Access runtime exceptions from method references.
  * Fix: Subexpressions are evaluated properly.


Version 0.3.0 *(2016-04-26)*
----------------------------

  * New: Column definitions use SQLite types and can have additional 'AS' constraint to specify java type.
  * New: Bug reports can be sent from the IDE.
  * Fix: Autocomplete functions properly.
  * Fix: SQLDelight model files update on .sq file edit.
  * Removed: Attached databases no longer supported.


Version 0.2.2 *(2016-03-07)*
----------------------------

 * New: Compile-time validation of the columns used by insert, update, delete, index, and trigger statements.
 * Fix: Don't crash IDE plugin on file move/create.


Version 0.2.1 *(2016-03-07)*
----------------------------

 * New: Ctrl+`/` (Cmd+`/` on OSX) toggles comment of the selected line(s).
 * New: Compile-time validation of the columns used by SQL queries.
 * Fix: Support Windows paths in both the IDE and Gradle plugin.


Version 0.2.0 *(2016-02-29)*
----------------------------

 * New: Added copy constructor to Marshal class.
 * New: Update to Kotlin 1.0 final.
 * Fix: Report 'sqldelight' folder structure problems in a non-failing way.
 * Fix: Forbid columns named `table_name`. Their generated constant clashes with the table name constant.
 * Fix: Ensure IDE plugin generates model classes immediately and regardless of whether `.sq` files were opened.
 * Fix: Support Windows paths in both the IDE and Gradle plugin.


Version 0.1.2 *(2016-02-13)*
----------------------------

 * Fix: Remove code which prevented the Gradle plugin from being used in most projects.
 * Fix: Add missing compiler dependency on the Antlr runtime.


Version 0.1.1 *(2016-02-12)*
----------------------------

 * Fix: Ensure the Gradle plugin points to the same version of the runtime as itself.


Version 0.1.0 *(2016-02-12)*
----------------------------

Initial release.
