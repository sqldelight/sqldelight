Change Log
==========

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
