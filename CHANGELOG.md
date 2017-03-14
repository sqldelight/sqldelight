Change Log
==========

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
