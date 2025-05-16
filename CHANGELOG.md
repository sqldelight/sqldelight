# Change Log

## [2.0.2] - 2024-04-05

### Added
- [PostgreSQL Dialect] Add PostgreSQL STRING_AGG function (#4950 by [André Danielsson][anddani])
- [PostgreSQL Dialect] Add SET statement to pg dialect (#4927 by [Bastien de Luca][de-luca])
- [PostgreSQL Dialect] Add PostgreSql alter column sequence parameters (#4916 by [Griffio][griffio])
- [PostgreSQL Dialect] Add postgresql alter column default support for insert statement (#4912 by [Griffio][griffio])
- [PostgreSQL Dialect] Add PostgreSql alter sequence and drop sequence (#4920 by [Griffio][griffio])
- [PostgreSQL Dialect] Add Postgres Regex function definitions (#5025 by [Marius Volkhart][MariusV])
- [PostgreSQL Dialect] Add grammar for GIN (#5027 by [Griffio][griffio])

### Changed
- [IDE Plugin] Minimum version of 2023.1 / Android Studio Iguana
- [Compiler] Allow overriding the type nullability in encapsulatingType (#4882 by [Eliezer Graber][eygraber])
- [Compiler] Inline the column names for SELECT *
- [Gradle Plugin] switch to processIsolation (#5068 by [Emeka Nwagu][nwagu])
- [Android Runtime] Increase Android minSDK to 21 (#5094 by [Philip Wedemann][hfhbd])
- [Drivers] Expose more JDBC/R2DBC statement methods for dialect authors (#5098 by [Philip Wedemann][hfhbd])

### Fixed
- [PostgreSQL Dialect] Fix postgresql alter table alter column (#4868 by [Griffio][griffio])
- [PostgreSQL Dialect] Fix 4448 missing import for table model (#4885 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 4932 postgresql default constraint functions (#4934 by [Griffio][griffio])
- [PostgreSQL Dialect] fixes 4879 postgresql class-cast error in alter table rename column during migrations (#4880 by [Griffio][griffio])
- [PostgreSQL Dialect] Fix 4474 PostgreSql create extension (#4541 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 5018 PostgreSql add Primary Key not nullable types (#5020 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 4703 aggregate expressions (#5071 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 5028 PostgreSql json (#5030 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 5040 PostgreSql json operators (#5041 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes json operator binding for 5040 (#5100 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 5082 tsvector (#5104 by [Griffio][griffio])
- [PostgreSQL Dialect] Fixes 5032 column adjacency for PostgreSql UPDATE FROM statement (#5035 by [Griffio][griffio])
- [SQLite Dialect] fixes 4897 sqlite alter table rename column (#4899 by [Griffio][griffio])
- [IDE Plugin] Fix error handler crash (#4988 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] BugSnag fails to init in IDEA 2023.3 (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] PluginException when opening .sq file in IntelliJ via plugin (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Dont bundle the kotlin lib into the intellij plugin as its already a plugin dependency (#5126)
- [IDE Plugin] Use the extensions array instead of stream (#5127)

## [2.0.1] - 2023-12-01

### Added
- [Compiler] Add support multi-column-expr when doing a SELECT (#4453 by [Adriel Martinez][Adriel-M])
- [PostgreSQL Dialect] Add support for PostgreSQL CREATE INDEX CONCURRENTLY (#4531 by [Griffio][griffio])
- [PostgreSQL Dialect] Allow PostgreSQL CTEs auxiliary statements to reference each other (#4493 by [Griffio][griffio])
- [PostgreSQL Dialect] Add support for PostgreSQL types for binary expr and sum (#4539 by [Adriel Martinez][Adriel-M])
- [PostgreSQL Dialect] Add support for PostgreSQL SELECT DISTINCT ON syntax (#4584 by [Griffio][griffio])
- [PostgreSQL Dialect] Add support for PostgreSQL JSON functions in SELECT statements (#4590 by [Marius Volkhart][MariusV])
- [PostgreSQL Dialect] Add generate_series PostgreSQL function (#4717 by [Griffio][griffio])
- [PostgreSQL Dialect] Add additional Postgres String function definitions (#4752 by [Marius Volkhart][MariusV])
- [PostgreSQL Dialect] Add DATE PostgreSQL type to min and max aggregate functions (#4816 by [André Danielsson][anddani])
- [PostgreSQL Dialect] Add PostgreSql temporal types to SqlBinaryExpr (#4657 by [Griifio][griffio])
- [PostgreSQL Dialect] Add TRUNCATE to postgres dialect (#4817 by [Bastien de Luca][de-luca])
- [SQLite 3.35 Dialect] Allow multiple ON CONFLICT clauses that are evaluated in order (#4551 by [Griffio][griffio])
- [JDBC Driver] Add Language annotations for more pleasant SQL editing (#4602 by [Marius Volkhart][MariusV])
- [Native Driver] Native-driver: add support for linuxArm64 (#4792 by [Philip Wedemann][hfhbd])
- [Android Driver] Add a windowSizeBytes parameter to AndroidSqliteDriver (#4804 by [Benoit Lubek][BoD])
- [Paging3 Extension] feat: add initialOffset for OffsetQueryPagingSource (#4802 by [Mohamad Jaara][MohamadJaara])

### Changed
- [Compiler] Prefer Kotlin types where appropriate (#4517 by [Eliezer Graber][eygraber])
- [Compiler] When doing a value type insert always include the column names (#4864)
- [PostgreSQL Dialect] Remove experimental status from PostgreSQL dialect (#4443 by [Philip Wedemann][hfhbd])
- [PostgreSQL Dialect] Update docs for PostgreSQL types (#4569 by [Marius Volkhart][MariusV])
- [R2DBC Driver] Optimize performance when handling integer data types in PostgreSQL (#4588 by [Marius Volkhart][MariusV])

### Removed
- [SQLite Javascript Driver] Remove sqljs-driver (#4613, #4670 by [Derek Ellis][dellisd])

### Fixed
- [Compiler] Fix compilation of grouped statements with returns and no parameters (#4699 by [Griffio][griffio])
- [Compiler] Bind arguments with SqlBinaryExpr (#4604 by [Griffio][griffio])
- [IDE Plugin] Use IDEA Project JDK if set (#4689 by [Griffio][griffio])
- [IDE Plugin] Fix "Unknown element type: TYPE_NAME" error in IDEA 2023.2 and greater (#4727)
- [IDE Plugin] Fixed some compatibility issues with 2023.2
- [Gradle Plugin] Correct documentation of verifyMigrationTask Gradle task (#4713 by [Josh Friend][joshfriend])
- [Gradle Plugin] Add Gradle task output message to help users generate a database before verifying a database (#4684 by [Jingwei][jingwei99])
- [PostgreSQL Dialect] Fix the renaming of PostgreSQL columns multiple times (#4566 by [Griffio][griffio])
- [PostgreSQL Dialect] Fix 4714 postgresql alter column nullability (#4831 by [Griffio][griffio])
- [PostgreSQL Dialect] Fix 4837 alter table alter column (#4846 by [Griffio][griffio])
- [PostgreSQL Dialect] Fix 4501 PostgreSql sequence (#4528 by [Griffio][griffio])
- [SQLite Dialect] Allow JSON binary operator to be used on a column expression (#4776 by [Eliezer Graber][eygraber])
- [SQLite Dialect] Update From false positive for multiple columns found with name (#4777 by [Eliezer Graber][eygraber])
- [Native Driver] Support named in-memory databases (#4662 by [Matthew Nelson][05nelsonm])
- [Native Driver] Ensure thread safety for query listener collection (#4567 by [Kevin Galligan][kpgalligan])
- [JDBC Driver] Fix a connection leak in the ConnectionManager (#4589 by [Marius Volkhart][MariusV])
- [JDBC Driver] Fix JdbcSqliteDriver url parsing when choosing ConnectionManager type (#4656 by [Matthew Nelson][05nelsonm])

## [2.0.0] - 2023-07-26

### Added
- [MySQL Dialect] MySQL: support timestamp/bigint in IF expression (#4329 by [Mike Gershunovsky][shellderp])
- [MySQL Dialect] MySQL: Add now (#4431 by [Philip Wedemann][hfhbd])
- [Web Driver] Enable NPM package publishing (#4364)
- [IDE Plugin] Allow users to show the stacktrace when the gradle tooling connect fails (#4383)

### Changed
- [Sqlite Driver] Simplify using schema migrations for JdbcSqliteDriver (#3737 by [Lukáš Moravec][morki])
- [R2DBC Driver] Real async R2DBC cursor (#4387 by [Philip Wedemann][hfhbd])

### Fixed
- [IDE Plugin] Dont instantiate the database project service until needed (#4382)
- [IDE Plugin] Handle process cancellation during find usages (#4340)
- [IDE Plugin] Fix IDE generation of async code (#4406)
- [IDE Plugin] Move assembly of the package structure to be one-time computed and off the EDT (#4417)
- [IDE Plugin] Use the correct stub index key for kotlin type resolution on 2023.2 (#4416)
- [IDE Plugin] Wait for the index to be ready before performing a search (#4419)
- [IDE Plugin] Dont perform a goto if the index is unavailable (#4420)
- [Compiler] Fix result expression for grouped statements (#4378)
- [Compiler] Don't use virtual table as interface type (#4427 by [Philip Wedemann][hfhbd])

## [2.0.0-rc02] - 2023-06-27

### Added
- [MySQL Dialect] support lowercase date types and min and max on date types (#4243 by [Mike Gershunovsky][shellderp])
- [MySQL Dialect] support mysql types for binary expr and sum (#4254 by [Mike Gershunovsky][shellderp])
- [MySQL Dialect] support unsigned ints without display width (#4306 by [Mike Gershunovsky][shellderp])
- [MySQL Dialect] Support LOCK IN SHARED MODE
- [PostgreSQL Dialect] Add boolean and Timestamp to min max (#4245 by [Griffio][griffio])
- [PostgreSQL Dialect] Postgres: Add window function support (#4283 by [Philip Wedemann][hfhbd])
- [Runtime] Add linuxArm64, androidNative and watchosDeviceArm targets to runtime (#4258 by [Philip Wedemann][hfhbd])
- [Paging Extension] Add linux and mingw x64 target to the paging extension (#4280 by [Cedric Hippmann][chippman])

### Changed
- [Gradle Plugin] Add automatic dialect support for Android API 34 (#4251)
- [Paging Extension] Add support for SuspendingTransacter in QueryPagingSource (#4292 by [Ilya Polenov][daio])
- [Runtime] Improve addListener api (#4244 by [Philip Wedemann][hfhbd])
- [Runtime] Use Long as migration version (#4297 by [Philip Wedemann][hfhbd])

### Fixed
- [Gradle Plugin] Use stable output path for generated source (#4269 by [Josh Friend][joshfriend])
- [Gradle Plugin] Gradle tweaks (#4222 by [Matthew Haughton][3flex])

## [2.0.0-rc01] - 2023-05-29

### Added
- [Paging] Add js browser target to paging extensions (#3843 by [Sean Proctor][sproctor])
- [Paging] Add iosSimulatorArm64 target to androidx-paging3 extension (#4117)
- [PostgreSQL Dialect] add support and test for gen_random_uuid() (#3855 by [David Wheeler][davidwheeler123])
- [PostgreSQL Dialect] Alter table add constraint postgres (#4116 by [Griffio][griffio])
- [PostgreSQL Dialect] Alter table add constraint check (#4120 by [Griffio][griffio])
- [PostgreSQL Dialect] Add postgreSql character length functions (#4121 by [Griffio][griffio])
- [PostgreSQL Dialect] Add postgreSql column default interval (#4142 by [Griffio][griffio])
- [PostgreSQL Dialect] Add postgreSql interval column result (#4152 by [Griffio][griffio])
- [PostgreSQL Dialect] Add postgreSql Alter Column (#4165 by [Griffio][griffio])
- [PostgreSQL Dialect] PostgreSQL: Add date_part (#4198 by [Philip Wedemann][hfhbd])
- [MySQL Dialect] Add sql char length functions (#4134 by [Griffio][griffio])
- [IDE Plugin] Add sqldelight directory suggestions (#3976 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Compact middle packages in project tree (#3992 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add join clause completion (#4086 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Create view intention and live template (#4074 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Warn about missing WHERE inside DELETE or UPDATE (#4058 by [Alexander Perfilyev][aperfilyev])
- [Gradle Plugin] Enable typesafe project accessors (#4005 by [Philip Wedemann][hfhbd])

### Changed
- [Gradle Plugin] Allow registering DriverInitializer for VerifyMigrationTask with ServiceLoader mechanism (#3986 by [Alex Doubov][C2H6O])
- [Gradle Plugin] Create explicit compiler env (#4079 by [Philip Wedemann][hfhbd])
- [JS Driver] Split web worker driver into separate artifact
- [JS Driver] Don't expose JsWorkerSqlCursor (#3874 by [Philip Wedemann][hfhbd])
- [JS Driver] Disable publication of the sqljs driver (#4108)
- [Runtime] Enforce that synchronous drivers require a synchronous schema initializer (#4013)
- [Runtime] Improve async support for Cursors (#4102)
- [Runtime] Remove deprecated targets (#4149 by [Philip Wedemann][hfhbd])
- [Runtime] Remove support for old MM (#4148 by [Philip Wedemann][hfhbd])

### Fixed
- [R2DBC Driver] R2DBC: Await closing the driver (#4139 by [Philip Wedemann][hfhbd])
- [Compiler] Include PRAGMAs from migrations in database create(SqlDriver) (#3845 by [Marius Volkhart][MariusV])
- [Compiler] Fix codegen for RETURNING clause (#3872 by [Marius Volkhart][MariusV])
- [Compiler] Dont generate types for virtual tables (#4015)
- [Gradle Plugin] Small Gradle plugin QoL improvements (#3930 by [Zac Sweers][zacsweers])
- [IDE Plugin] Fix unresolved kotlin types (#3924 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix for expand wildcard intention to work with qualifier (#3979 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Use available jdk if java home is missing (#3925 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix find usages on package names (#4010)
- [IDE Plugin] Dont show auto imports for invalid elements (#4008)
- [IDE Plugin] Do not resolve if a dialect is missing (#4009)
- [IDE Plugin] Ignore IDE runs of the compiler during an invalidated state (#4016)
- [IDE Plugin] Add support for IntelliJ 2023.1 (#4037 by [Madis Pink][madisp])
- [IDE Plugin] Rename named argument usage on column rename (#4027 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix add migration popup (#4105 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Disable SchemaNeedsMigrationInspection in migration files (#4106 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Use sql column name for migration generation instead of type name (#4112 by [Alexander Perfilyev][aperfilyev])

## [2.0.0-alpha05] - 2023-01-20

### Added
- [Paging] Multiplatform paging extension (by [Jeff Lockhart][jeffdgr8])
- [Runtime] Add fun modifier to Listener interface.
- [SQLite Dialect] Add SQLite 3.33 support (UPDATE FROM) (by [Eliezer Graber][eygraber]))
- [PostgreSQL Dialect] Support UPDATE FROM in postgresql (by [Eliezer Graber][eygraber]))

### Changed
- [RDBC Driver] Expose the connection (by [Philip Wedemann][hfhbd])
- [Runtime] Move migration callbacks into main `migrate` fun
- [Gradle Plugin] Hide Configurations from downstream projects
- [Gradle Plugin] Only shade Intellij (by [Philip Wedemann][hfhbd])
- [Gradle Plugin] Support Kotlin 1.8.0-Beta and add multi version Kotlin test (by [Philip Wedemann][hfhbd])

### Fixed
- [RDBC Driver] Use javaObjectType instead (by [Philip Wedemann][hfhbd])
- [RDBC Driver] Fix primitive null values in bindStatement (by [Philip Wedemann][hfhbd])
- [RDBC Driver] Support R2DBC 1.0 (by [Philip Wedemann][hfhbd])
- [PostgreSQL Dialect] Postgres: Fix Array without type parameter (by [Philip Wedemann][hfhbd])
- [IDE Plugin] Bump intellij to 221.6008.13 (by [Philip Wedemann][hfhbd])
- [Compiler] Resolve recursive origin table from pure views (by [Philip Wedemann][hfhbd])
- [Compiler] Use value classes from table foreign key clause (by [Philip Wedemann][hfhbd])
- [Compiler] Fix SelectQueryGenerator to support bind expression without parenthesis (by [Doogie Min][bellatoris])
- [Compiler] Fix duplicate generation of ${name}Indexes variables when using transactions (by [Andreas Sacher][sachera])

## [1.5.5] - 2023-01-20

This is a compatibility release for Kotlin 1.8 and IntelliJ 2021+, supporting JDK 17.

## [1.5.4] - 2022-10-06

This is a compatibility update for Kotlin 1.7.20 and AGP 7.3.0.

## [2.0.0-alpha04] - 2022-10-03

### Breaking Changes

- The Paging 3 extension API has changed to only allow int types for the count.
- The coroutines extension now requires a dispatcher to be passed in instead of defaulting.
- Dialect and Driver classes are final, use delegation instead.

### Added
- [HSQL Dialect] Hsql: Support using DEFAULT for generated columns in Insert (#3372 by [Philip Wedemann][hfhbd])
- [PostgreSQL Dialect] PostgreSQL: Support using DEFAULT for generated columns in INSERT  (#3373 by [Philip Wedemann][hfhbd])
- [PostgreSQL Dialect] Add NOW() to PostgreSQL (#3403 by [Philip Wedemann][hfhbd])
- [PostgreSQL Dialect] PostgreSQL Add NOT operator (#3504 by [Philip Wedemann][hfhbd])
- [Paging] Allow passing in CoroutineContext to *QueryPagingSource (#3384)
- [Gradle Plugin] Add better version catalog support for dialects (#3435)
- [Native Driver] Add callback to hook into DatabaseConfiguration creation of NativeSqliteDriver (#3512 by [Sven Jacobs][svenjacobs])

### Changed
- [Paging] Add a default dispatcher to the KeyedQueryPagingSource backed QueryPagingSource function (#3385)
- [Paging] Make OffsetQueryPagingSource only work with Int (#3386)
- [Async Runtime] Move await* to upper class ExecutableQuery (#3524 by [Philip Wedemann][hfhbd])
- [Coroutines Extensions] Remove default params to flow extensions (#3489)

### Fixed
- [Gradle Plugin] Update to Kotlin 1.7.20 (#3542 by [Zac Sweers][zacsweers])
- [R2DBC Driver] Adopt R2DBC changes which do not always send a value (#3525 by [Philip Wedemann][hfhbd])
- [HSQL Dialect] Fix failing sqlite VerifyMigrationTask with Hsql (#3380 by [Philip Wedemann][hfhbd])
- [Gradle Plugin] Convert tasks to use lazy configuration API (by [Matthew Haughton][3flex])
- [Gradle Plugin] Avoid NPEs in Kotlin 1.7.20 (#3398 by [Zac Sweers][ZacSweers])
- [Gradle Plugin] Fix description of squash migrations task (#3449)
- [IDE Plugin] Fix NoSuchFieldError in newer Kotlin plugins (#3422 by [Madis Pink][madisp])
- [IDE Plugin] IDEA: UnusedQueryInspection - fix ArrayIndexOutOfBoundsException. (#3427 by [Niklas Baudy][vanniktech])
- [IDE Plugin] Use reflection for old kotlin plugin references
- [Compiler] Custom dialect with extension function don't create imports (#3338 by [Philip Wedemann][hfhbd])
- [Compiler] Fix escaping CodeBlock.of("${CodeBlock.toString()}") (#3340 by [Philip Wedemann][hfhbd])
- [Compiler] Await async execute statements in migrations (#3352)
- [Compiler] Fix AS (#3370 by [Philip Wedemann][hfhbd])
- [Compiler] `getObject`  method supports automatic filling of the actual type. (#3401 by [Rob X][robx])
- [Compiler] Fix codegen for async grouped returning statements (#3411)
- [Compiler] Infer the Kotlin type of bind parameter, if possible, or fail with a better error message (#3413 by [Philip Wedemann][hfhbd])
- [Compiler] Don't allow ABS("foo") (#3430 by [Philip Wedemann][hfhbd])
- [Compiler] Support inferring kotlin type from other parameters (#3431 by [Philip Wedemann][hfhbd])
- [Compiler] Always create the database implementation (#3540 by [Philip Wedemann][hfhbd])
- [Compiler] Relax javaDoc and add it to custom mapper function too (#3554 [Philip Wedemann][hfhbd])
- [Compiler] Fix DEFAULT in binding (by [Philip Wedemann][hfhbd])
- [Paging] Fix Paging 3 (#3396)
- [Paging] Allow construction of OffsetQueryPagingSource with Long (#3409)
- [Paging] Don't statically swap Dispatchers.Main (#3428)

## [2.0.0-alpha03] - 2022-06-17

### Breaking Changes

- Dialects are now references like actual gradle dependencies.
```groovy
sqldelight {
  MyDatabase {
    dialect("app.cash.sqldelight:postgres-dialect:2.0.0-alpha03")
  }
}
```
- The `AfterVersionWithDriver` type was removed in favour of `AfterVersion` which now always has the driver.
- The `Schema` type is no longer a subtype of `SqlDriver`
- `PreparedStatement` APIs are now called with zero-based indexes.

### Added
- [IDE Plugin] Added support for running SQLite, MySQL, and PostgreSQL commands against a running database (#2718 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add support for the android studio DB inspector (#3107 by [Alexander Perfilyev][aperfilyev])
- [Runtime] Add support for async drivers (#3168 by [Derek Ellis][dellisd])
- [Native Driver] Support new kotlin native memory model (#3177 by [Kevin Galligan][kpgalligan])
- [JS Driver] Add a driver for SqlJs workers (#3203 by [Derek Ellis][dellisd])
- [Gradle Plugin] Expose the classpath for SQLDelight tasks
- [Gradle Plugin] Add a gradle task for squashing migrations
- [Gradle Plugin] Add a flag to ignore schema definitions during migration checks
- [MySQL Dialect] Support FOR SHARE and FOR UPDATE in MySQL (#3098)
- [MySQL Dialect] Support MySQL index hints (#3099)
- [PostgreSQL Dialect] Add date_trunc (#3295 by [Philip Wedemann][hfhbd])
- [JSON Extensions] Support JSON table functions (#3090)

### Changed
- [Runtime] Remove the AfterVersion type without the driver (#3091)
- [Runtime] Move Schema type to top-level
- [Runtime] Open dialect and resolver to support 3rd party implementations (#3232 by [Philip Wedemann][hfhbd])
- [Compiler] Include the dialect used to compile in failure reports (#3086)
- [Compiler] Skip unused adapters (#3162 by [Eliezer Graber][eygraber])
- [Compiler] Use zero based index in PrepareStatement (#3269 by [Philip Wedemann][hfhbd])
- [Gradle Plugin] Also make the dialect a proper gradle dependency instead of a string (#3085)
- [Gradle Plugin] Gradle Verify Task: Throw when missing database file. (#3126 by [Niklas Baudy][vanniktech])

### Fixed
- [Gradle Plugin] Minor cleanups and tweaks to the Gradle plugin (#3171 by [Matthew Haughton][3flex])
- [Gradle Plugin] Dont use an AGP string for the generated directory
- [Gradle Plugin] Use AGP namespace attribute (#3220)
- [Gradle Plugin] Do not add kotlin-stdlib as a runtime dependency of the Gradle plugin (#3245 by [Martin Bonnin][mbonnin])
- [Gradle Plugin] Simplify the multiplatform configuration (#3246 by [Martin Bonnin][mbonnin])
- [Gradle Plugin] Support js only projects (#3310 by [Philip Wedemann][hfhbd])
- [IDE Plugin] Use java home for gradle tooling API (#3078)
- [IDE Plugin] Load the JDBC driver on the correct classLoader inside the IDE plugin (#3080)
- [IDE Plugin] Mark the file element as null before invalidating to avoid errors during already existing PSI changes (#3082)
- [IDE Plugin] Dont crash finding usages of the new table name in an ALTER TABLE statement (#3106)
- [IDE Plugin] Optimize the inspectors and enable them to fail silently for expected exception types (#3121)
- [IDE Plugin] Delete files that should be generated directories (#3198)
- [IDE Plugin] Fix a not-safe operator call
- [Compiler] Ensure updates and deletes with RETURNING statements execute queries. (#3084)
- [Compiler] Correctly infer argument types in compound selects (#3096)
- [Compiler] Common tables do not generate data classes so dont return them (#3097)
- [Compiler] Find the top migration file faster (#3108)
- [Compiler] Properly inherit nullability on the pipe operator
- [Compiler] Support the iif ANSI SQL function
- [Compiler] Don't generate empty query files (#3300 by [Philip Wedemann][hfhbd])
- [Compiler] Fix adapter with question mark only (#3314 by [Philip Wedemann][hfhbd])
- [PostgreSQL Dialect] Postgres primary key columns are always non-null (#3092)
- [PostgreSQL Dialect] Fix copy with same name in multiple tables (#3297 by [Philip Wedemann][hfhbd])
- [SQLite 3.35 Dialect] Only show an error when dropping an indexed column from the altered table (#3158 by [Eliezer Graber][eygraber])

## [2.0.0-alpha02] - 2022-04-13

### Breaking Changes

- You'll need to replace all occurrences of `app.cash.sqldelight.runtime.rx` with `app.cash.sqldelight.rx2`

### Added
- [Compiler] Support returning at the end of a grouped statement
- [Compiler] Support compiler extensions via dialect modules and add a SQLite JSON extension (#1379, #2087)
- [Compiler] Support PRAGMA statements which return a value (#1106)
- [Compiler] Support generating value types for marked columns
- [Compiler] Add support for optimistic locks and validation (#1952)
- [Compiler] Support multi-update statements
- [PostgreSQL] Support postgres returning statements
- [PostgreSQL] Support postgres date types
- [PostgreSQL] Support pg intervals
- [PostgreSQL] Support PG Booleans and fix inserts on alter tables
- [PostgreSQL] Support optional limits in Postgres
- [PostgreSQL] Support PG BYTEA type
- [PostgreSQL] Add a test for postgres serials
- [PostgreSQL] Support for update postgres syntax
- [PostgreSQL] Support PostgreSQL array types
- [PostgreSQL] Properly store/retrieve UUID types in PG
- [PostgreSQL] Support PostgreSQL NUMERIC type (#1882)
- [PostgreSQL] Support returning queries inside of common table expressions (#2471)
- [PostgreSQL] Support json specific operators
- [PostgreSQL] Add Postgres Copy (by [Philip Wedemann][hfhbd])
- [MySQL] Support MySQL Replace
- [MySQL] Support NUMERIC/BigDecimal MySQL types (#2051)
- [MySQL] Support MySQL truncate statement
- [MySQL] Support json specific operators in Mysql (by [Eliezer Graber][eygraber])
- [MySQL] Support MySql INTERVAL (#2969 by [Eliezer Graber][eygraber])
- [HSQL] Add HSQL Window functionality
- [SQLite] Don't replace equality checks for nullable parameters in a WHERE (#1490 by [Eliezer Graber][eygraber])
- [SQLite] Support Sqlite 3.35 returning statements (#1490 by [Eliezer Graber][eygraber])
- [SQLite] Support GENERATED clause
- [SQLite] Add support for Sqlite 3.38 dialect (by [Eliezer Graber][eygraber])

### Changed
- [Compiler] Clean up generated code a bit
- [Compiler] Forbid usage of table parameters in grouped statements (#1822)
- [Compiler] Put grouped queries inside a transaction (#2785)
- [Runtime] Return the updated row count from the drivers execute method
- [Runtime] Confine SqlCursor to the critical section accessing the connection. (#2123 by [Anders Ha][andersio])
- [Gradle Plugin] Compare schema definitions for migrations (#841)
- [PostgreSQL] Disallow double quotes for PG
- [MySQL] Error on usage of == in MySQL (#2673)

### Fixed
- [Compiler] Same adapter type from different tables causing a compilation error in 2.0 alpha
- [Compiler] Problem compiling upsert statement (#2791)
- [Compiler] Query result should use tables in the select if there are multiple matches (#1874, #2313)
- [Compiler] Support updating a view which has a INSTEAD OF trigger (#1018)
- [Compiler] Support from and for in function names
- [Compiler] Allow SEPARATOR keyword in function expressions
- [Compiler] Cannot access ROWID of aliased table in ORDER BY
- [Compiler] Aliased column name is not recognized in HAVING clause in MySQL
- [Compiler] Erroneous 'Multiple columns found' error
- [Compiler] Unable to set PRAGMA locking_mode = EXCLUSIVE;
- [PostgreSQL] Postgresql rename column
- [MySQL] UNIX_TIMESTAMP, TO_SECONDS, JSON_ARRAYAGG MySQL functions not recognized
- [SQLite] fix SQLite window functionality
- [IDE Plugin] Run the goto handler in an empty progress indicator (#2990)
- [IDE Plugin] Ensure the highlight visitor doesnt run if the project isnt configured (#2981, #2976)
- [IDE Plugin] Ensure transitive generated code is also updated in the IDE (#1837)
- [IDE Plugin] Invalidate indexes when updating the dialect

## [2.0.0-alpha01] - 2022-03-31

This is the first alpha release for 2.0 and has some breaking changes. We expect more ABI breaking changes to come so don't publish any libraries with dependencies on this release (applications should be fine).

### Breaking Changes

- First, you'll need to replace all occurrences of `com.squareup.sqldelight` with `app.cash.sqldelight`
- Second, you'll need to replace all occurrences of `app.cash.sqldelight.android` with `app.cash.sqldelight.driver.android`
- Third, you'll need to replace all occurrences of `app.cash.sqldelight.sqlite.driver` with `app.cash.sqldelight.driver.jdbc.sqlite`
- Fourth, you'll need to replace all occurrences of `app.cash.sqldelight.drivers.native` with `app.cash.sqldelight.driver.native`
- The IDE plugin must be updated to a 2.X version, which can be found in the [alpha or eap channel](https://plugins.jetbrains.com/plugin/8191-sqldelight/versions/alpha)
- Dialects are now dependencies which you can specify within gradle:

```gradle
sqldelight {
  MyDatabase {
    packageName = "com.example"
    dialect = "app.cash.sqldelight:mysql-dialect:2.0.0-alpha01"
  }
}
```

The currently supported dialects are `mysql-dialect`, `postgresql-dialect`, `hsql-dialect`, `sqlite-3-18-dialect`, `sqlite-3-24-dialect`, `sqlite-3-25-dialect`, `sqlite-3-30-dialect`, and `sqlite-3-35-dialect`

- Primitive types must now be imported (for example `INTEGER AS Boolean` you have to `import kotlin.Boolean`), some previously supported types now need an adapter. Primitive adapters are available in `app.cash.sqldelight:primitive-adapters:2.0.0-alpha01` for most conversions (like `IntColumnAdapter` for doing `Integer AS kotlin.Int`).

### Added
- [IDE Plugin] Basic suggested migration (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add import hint action (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add kotlin class completion (by [Alexander Perfilyev][aperfilyev])
- [Gradle Plugin] Add shortcut for Gradle type safe project accessors (by [Philip Wedemann][hfhbd])
- [Compiler] Customize codegen based on dialect (by [Marius Volkhart][MariusV])
- [JDBC Driver] Add common types to JdbcDriver (by [Marius Volkhart][MariusV])
- [SQLite] Add support for the sqlite 3.35 (by [Eliezer Graber][eygraber])
- [SQLite] Add support for ALTER TABLE DROP COLUMN (by [Eliezer Graber][eygraber])
- [SQLite] Add support for Sqlite 3.30 dialect (by [Eliezer Graber][eygraber])
- [SQLite] Support NULLS FIRST/LAST in sqlite (by [Eliezer Graber][eygraber])
- [HSQL] Add HSQL support for generated clause (by [Marius Volkhart][MariusV])
- [HSQL] Add support for named parameters in HSQL (by [Marius Volkhart][MariusV])
- [HSQL] Customize the HSQL insert query (by [Marius Volkhart][MariusV])

### Changed
- [Everything] Package name has changed from com.squareup.sqldelight to app.cash.sqldelight.
- [Runtime] Move dialects into their own isolated gradle modules
- [Runtime] Switch to driver-implemented query notifications.
- [Runtime] Extract default column adapters to separate module (#2056, #2060)
- [Compiler] Let modules generate the queries implementations instead of redoing it in each module
- [Compiler] Remove the custom toString generation of generated data classes. (by [Paul Woitaschek][PaulWoitaschek])
- [JS Driver] Remove sql.js dependency from sqljs-driver (by [Derek Ellis][dellisd])
- [Paging] Remove the android paging 2 extension
- [IDE Plugin] Add an editor banner while SQLDelight is syncing (#2511)
- [IDE Plugin] Minimum supported IntelliJ version is 2021.1

### Fixed
- [Runtime] Flatten listener list to reduce allocations and pointer chasing. (by [Anders Ha][andersio])
- [IDE Plugin] Fix error message to allow jumping to error (by [Philip Wedemann][hfhbd])
- [IDE Plugin] Add missing inspection descriptions (#2768 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix exception in GotoDeclarationHandler (#2531, #2688, #2804 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Highlight import keyword (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix unresolved kotlin types (#1678 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix highlighting for unresolved package (#2543 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Dont attempt to inspect mismatched columns if the project index is not yet initialized
- [IDE Plugin] Dont initialize the file index until a gradle sync has occurred
- [IDE Plugin] Cancel the SQLDelight import if a gradle sync begins
- [IDE Plugin] Regenerate the database outside of the thread an undo action is performed on
- [IDE Plugin] If a reference cannot be resolves use a blank java type
- [IDE Plugin] Correctly move off the main thread during file parsing and only move back on to write
- [IDE Plugin] Improve compatibility with older IntelliJ versions (by [Matthew Haughton][3flex])
- [IDE Plugin] Use faster annotation API
- [Gradle Plugin] Explicitly support js/android plugins when adding runtime (by [Zac Sweers][ZacSweers])
- [Gradle Plugin] Register migration output task without derviving schemas from migrations (#2744 by [Kevin Cianfarini][kevincianfarini])
- [Gradle Plugin] If the migration task crashes, print the file it crashed running
- [Gradle Plugin] Sort files when generating code to ensure idempotent outputs (by [Zac Sweers][ZacSweers])
- [Compiler] Use faster APIs for iterating files and dont explore the entire PSI graph
- [Compiler] Add keyword mangling to select function parameters (#2759 by [Alexander Perfilyev][aperfilyev])
- [Compiler] Fix packageName for migration adapter (by [Philip Wedemann][hfhbd])
- [Compiler] Emit annotations on properties instead of types (#2798 by [Alexander Perfilyev][aperfilyev])
- [Compiler] Sort arguments before passing to a Query subtype (#2379 by [Alexander Perfilyev][aperfilyev])

## [1.5.3] - 2021-11-23
### Added
- [JDBC Driver] Open JdbcDriver for 3rd party driver implementations (#2672 by [Philip Wedemann][hfhbd])
- [MySQL Dialect] Add missing functions for time increments (#2671 by [Sam Doward][sdoward])
- [Coroutines Extension] Add M1 targets for coroutines-extensions (by [Philip Dukhov][PhilipDukhov])

### Changed
- [Paging3 Extension] Distribute sqldelight-android-paging3 as JAR instead of AAR (#2634 by [Marco Romano][julioromano])
- Property names which are also soft keywords will now be suffixed with underscores. For instance `value` will be exposed as `value_`

### Fixed
- [Compiler] Don't extract variables for duplicate array parameters (by [Alexander Perfilyev][aperfilyev])
- [Gradle Plugin] add kotlin.mpp.enableCompatibilityMetadataVariant. (#2628 by [Martin Bonnin][martinbonnin])
- [IDE Plugin] Find usages processing requires a read action


## [1.5.2] - 2021-10-12
### Added
- [Gradle Plugin] HMPP support (#2548 by [Martin Bonnin][martinbonnin])
- [IDE Plugin] Add NULL comparison inspection (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add inspection suppressor (#2519 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Mixed named and positional parameters inspection (by [Alexander Perfilyev][aperfilyev])
- [SQLite Driver] Add mingwX86 target. (#2558 by [Nikita Kozhemyakin][enginegl])
- [SQLite Driver] Add M1 targets
- [SQLite Driver] Add linuxX64 support (#2456 by [Cedric Hippmann][chippmann])
- [MySQL Dialect] Add ROW_COUNT function to mysql (#2523)
- [PostgreSQL Dialect] postgres rename, drop column (by [Juan Liska][pabl0rg])
- [PostgreSQL Dialect] PostgreSQL grammar doesn't recognize CITEXT
- [PostgreSQL Dialect] Include TIMESTAMP WITH TIME ZONE and TIMESTAMPTZ
- [PostgreSQL Dialect] Add grammar for PostgreSQL GENERATED columns
- [Runtime] Provide SqlDriver as a parameter to AfterVersion (#2534, 2614 by [Ahmed El-Helw][ahmedre])

### Changed
- [Gradle Plugin] explicitely require Gradle 7.0 (#2572 by [Martin Bonnin][martinbonnin])
- [Gradle Plugin] Make VerifyMigrationTask support Gradle's up-to-date checks (#2533 by [Matthew Haughton][3flex])
- [IDE Plugin] Don't warn with "Join compares two columns of different types" when joining nullable with non-nullable type (#2550 by [Piotr Chmielowski][pchmielowski])
- [IDE Plugin] Clarify the error for the lowercase 'as' in column type (by [Alexander Perfilyev][aperfilyev])

### Fixed
- [IDE Plugin] Do not reparse under a new dialect if the project is already disposed (#2609)
- [IDE Plugin] If the associated virtual file is null, the module is null (#2607)
- [IDE Plugin] Avoid crashing during the unused query inspection (#2610)
- [IDE Plugin] Run the database sync write inside of a write action (#2605)
- [IDE Plugin] Let the IDE schedule SQLDelight syncronization
- [IDE Plugin] Fix npe in JavaTypeMixin (#2603 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix IndexOutOfBoundsException in MismatchJoinColumnInspection (#2602 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add description for UnusedColumnInspection (#2600 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Wrap PsiElement.generatedVirtualFiles into read action (#2599 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Remove unnecessary nonnull cast (#2596)
- [IDE Plugin] Properly handle nulls for find usages (#2595)
- [IDE Plugin] Fix IDE autocomplete for generated files for Android (#2573 by [Martin Bonnin][martinbonnin])
- [IDE Plugin] Fix npe in SqlDelightGotoDeclarationHandler (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Mangle kotlin keywords in arguments inside insert stmt (#2433 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix npe in SqlDelightFoldingBuilder (#2382 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Catch ClassCastException in CopyPasteProcessor (#2369 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix update live template (by [Ilias Redissi][IliasRedissi])
- [IDE Plugin] Adds descriptions to intention actions (#2489 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix exception in CreateTriggerMixin if table is not found (by [Alexander Perfilyev][aperfilyev])
- [Compiler] Topologically sort table creation statemenets
- [Compiler] Stop invoking `forDatabaseFiles` callback on directories (#2532)
- [Gradle Plugin] Propagate generateDatabaseInterface task dependency to potential consumers (#2518 by [Martin Bonnin][martinbonnin])


## [1.5.1] - 2021-07-16
### Added
- [PostgreSQL Dialect] PostgreSQL JSONB and ON Conflict Do Nothing (by [Andrew Stewart][satook])
- [PostgreSQL Dialect] Adds support for PostgreSQL ON CONFLICT (column, ...) DO UPDATE (by [Andrew Stewart][satook])
- [MySQL Dialect] Support MySQL generated columns (by [Jeff Gulbronson][JeffG])
- [Native Driver] Add watchosX64 support
- [IDE Plugin] Add parameter types and annotations (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add action to generate 'select all' query (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Show column types in autocomplete (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add icons to autocomplete (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add action to generate 'select by primary key' query (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add action to generate 'insert into' query (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add highlighting for column names, stmt identifiers, function names (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add remaining query generation actions (#489 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Show parameter hints from insert-stmt (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Table alias intention action (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Qualify column name intention (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Go to declaration for kotlin property (by [Alexander Perfilyev][aperfilyev])

### Changed
- [Native Driver] Improve native transaction performance by avoiding freezing and shareable data structures when possible (by [Anders Ha][andersio])
- [Paging 3] Bump Paging3 version to 3.0.0 stable
- [JS Driver] Upgrade sql.js to 1.5.0

### Fixed
- [JDBC SQLite Driver] Call close() on connection before clearing the ThreadLocal (#2444 by [Hannes Struß][hannesstruss])
- [RX extensions] Fix subscription / disposal race leak (#2403 by [Pierre Yves Ricau][pyricau])
- [Coroutines extension] Ensure we register query listener before notifying
- [Compiler] Sort notifyQueries to have consistent kotlin output file (by [Jiayu Chen][thomascjy])
- [Compiler] Don't annotate select query class properties with @JvmField (by [Eliezer Graber][eygraber])
- [IDE Plugin] Fix import optimizer (#2350 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix unused column inspection (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add nested classes support to import inspection and class annotator (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix npe in CopyPasteProcessor (#2363 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix crash in InlayParameterHintsProvider (#2359 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Fix insertion of blank lines when copy-pasting any text into create table stmt (#2431 by [Alexander Perfilyev][aperfilyev])


## [1.5.0] - 2021-04-23
### Added
- [SQLite Javascript Driver] Enable sqljs-driver publication (#1667 by [Derek Ellis][dellisd])
- [Paging3 Extension] Extension for Android Paging 3 Library (#1786 by [Kevin Cianfarini][kevincianfarini])


## [1.5.0] - 2021-04-23
### Added
- [SQLite Javascript Driver] Enable sqljs-driver publication (#1667 by [Derek Ellis][dellisd])
- [Paging3 Extension] Extension for Android Paging 3 Library (#1786 by [Kevin Cianfarini][kevincianfarini])
- [MySQL Dialect] Adds support for mysql's ON DUPLICATE KEY UPDATE conflict resolution. (by [Ryan Harter][rharter])
- [SQLite Dialect] Add compiler support for SQLite offsets() (by [Quinton Roberts][qjroberts])
- [IDE Plugin] Add import quick fix for unknown type (#683 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add unused import inspection (#1161 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add unused query inspection (by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Add unused column inspection (#569 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Automatically bring imports on copy/paste (#684 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Pop a balloon when there are incompatibilities between gradle/intellij plugin versions
- [IDE Plugin] Insert Into ... VALUES(?) parameter hints (#506 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] Inline parameter hints (by [Alexander Perfilyev][aperfilyev])
- [Runtime] Include an API in the runtime for running migrations with callbacks (#1844)

### Changed
- [Compiler] Smart cast "IS NOT NULL" queries (#867)
- [Compiler] Protect against keywords that will fail at runtime (#1471, #1629)
- [Gradle Plugin] Reduce size of gradle plugin from 60mb -> 13mb.
- [Gradle Plugin] Properly support android variants, and remove support for KMM target-specific sql (#1039)
- [Gradle Plugin] Pick a minimum sqlite version based on minsdk (#1684)
- [Native Driver] Native driver connection pool and performance updates

### Fixed
- [Compiler] NBSP before lambdas (by [Benoît Quenaudon][oldergod])
- [Compiler] Fix incompatible types in generated bind* and cursor.get* statements
- [Compiler] SQL clause should persist adapted type (#2067)
- [Compiler] Column with only NULL keyword should be nullable
- [Compiler] Dont generate mapper lambda with type annotations (#1957)
- [Compiler] If custom queries would clash, use the file name as an additional package suffix (#1057, #1278)
- [Compiler] Ensure foreign key cascades cause query listeners to be notified (#1325, #1485)
- [Compiler] If unioning two of the same type, return the table type (#1342)
- [Compiler] Ensure params to ifnull and coalesce can be nullable (#1263)
- [Compiler] Correctly use query-imposed nullability for expressions
- [MySQL Dialect] Support MySQL if statements
- [PostgreSQL Dialect] Retrieve NUMERIC and DECIMAL as Double in PostgreSQL (#2118)
- [SQLite Dialect] UPSERT notifications should account for BEFORE/AFTER UPDATE triggers. (#2198 by [Anders Ha][andersio])
- [SQLite Driver] Use multiple connections for threads in the SqliteDriver unless we are in memory (#1832)
- [JDBC Driver] JDBC Driver assumes autoCommit is true (#2041)
- [JDBC Driver] Ensure that we close connections on exception (#2306)
- [IDE Plugin] Fix GoToDeclaration/FindUsages being broken on Windows due to path separator bug (#2054 by [Angus Holder][AngusH])
- [IDE Plugin] Ignore gradle errors instead of crashing in the IDE.
- [IDE Plugin] If a sqldelight file is moved to a non-sqldelight module, do not attempt codegen
- [IDE Plugin] Ignore codegen errors in IDE
- [IDE Plugin] Ensure that we dont try to negatively substring (#2068)
- [IDE Plugin] Also ensure project is not disposed before running gradle action (#2155)
- [IDE Plugin] Arithmetic on nullable types should also be nullable (#1853)
- [IDE Plugin] Make 'expand * intention' work with additional projections (#2173 by [Alexander Perfilyev][aperfilyev])
- [IDE Plugin] If kotlin resolution fails during GoTo, dont attempt to go to sqldelight files
- [IDE Plugin] If IntelliJ encounters an exception while sqldelight is indexing, dont crash
- [IDE Plugin] Handle exceptions that happen while detecting errors before codegen in the IDE
- [IDE Plugin] Make the IDE plugin compatible with Dynamic Plugins (#1536)
- [Gradle Plugin] Race condition generating a database using WorkerApi (#2062 by [Stéphane Nicolas][stephanenicolas])
- [Gradle Plugin] classLoaderIsolation prevents custom jdbc usage (#2048 by [Ben Asher][BenA])
- [Gradle Plugin] Improve missing packageName error message (by [Niklas Baudy][vanniktech])
- [Gradle Plugin] SQLDelight bleeds IntelliJ dependencies onto buildscript class path (#1998)
- [Gradle Plugin] Fix gradle build caching (#2075)
- [Gradle Plugin] Do not depend on kotlin-native-utils in Gradle plugin (by [Ilya Matveev][ilmat192])
- [Gradle Plugin] Also write the database if there are only migration files (#2094)
- [Gradle Plugin] Ensure diamond dependencies only get picked up once in the final compilation unit (#1455)

Also just a general shoutout to [Matthew Haughton][3flex] who did a lot of work to improve the SQLDelight infrastructure this release.

## [1.4.4] - 2020-10-08
### Added
- [PostgreSQL Dialect] Support data-modifying statements in WITH
- [PostgreSQL Dialect] Support substring function
- [Gradle Plugin] Added verifyMigrations flag for validating migrations during SQLDelight compilation (#1872)

### Changed
- [Compiler] Flag SQLite specific functions as unknown in non-SQLite dialects
- [Gradle Plugin] Provide a warning when the sqldelight plugin is applied but no databases are configured (#1421)

### Fixed
- [Compiler] Report an error when binding a column name in an ORDER BY clause (#1187 by [Eliezer Graber][eygraber])
- [Compiler] Registry warnings appear when generating the db interface (#1792)
- [Compiler] Incorrect type inference for case statement (#1811)
- [Compiler] Provide better errors for migration files with no version (#2006)
- [Compiler] Required database type to marshal is incorrect for some database type ColumnAdapter's (#2012)
- [Compiler] Nullability of CAST (#1261)
- [Compiler] Lots of name shadowed warnings in query wrappers (#1946 by [Eliezer Graber][eygraber])
- [Compiler] Generated code is using full qualifier names (#1939)
- [IDE Plugin] Trigger sqldelight code gen from gradle syncs
- [IDE Plugin] Plugin not regenerating database interface when changing .sq files (#1945)
- [IDE Plugin] Issue when moving files to new packages (#444)
- [IDE Plugin] If theres nowhere to move the cursor, do nothing instead of crashing (#1994)
- [IDE Plugin] Use empty package name for files outside of a gradle project (#1973)
- [IDE Plugin] Fail gracefully for invalid types (#1943)
- [IDE Plugin] Throw a better error message when encountering an unknown expression (#1958)
- [Gradle Plugin] SQLDelight bleeds IntelliJ dependencies onto buildscript class path (#1998)
- [Gradle Plugin] "JavadocIntegrationKt not found" compilation error when adding method doc in *.sq file (#1982)
- [Gradle Plugin] SqlDeslight gradle plugin doesn't support Configuration Caching (CoCa). (#1947 by [Stéphane Nicolas][stephanenicolas])
- [SQLite JDBC Driver] SQLException: database in auto-commit mode (#1832)
- [Coroutines Extension] Fix IR backend for coroutines-extensions (#1918 by [Derek Ellis][dellisd])

## [1.4.3] - 2020-09-04
### Added
- [MySQL Dialect] Add support for MySQL last_insert_id function (by [Kelvin Law][lawkai])
- [PostgreSQL Dialect] Support SERIAL data type (by [Veyndan Stuart][VeyndanS] & [Felipe Lima][felipecsl])
- [PostgreSQL Dialect] Support PostgreSQL RETURNING (by [Veyndan Stuart][VeyndanS])

### Fixed
- [MySQL Dialect] Treat MySQL AUTO_INCREMENT as having a default value (#1823)
- [Compiler] Fix Upsert statement compiler error (#1809 by [Eliezer Graber][eygraber])
- [Compiler] Fix issue with invalid Kotlin being generated (#1925 by [Eliezer Graber][eygraber])
- [Compiler] Have a better error message for unknown functions (#1843)
- [Compiler] Expose string as the type for the second parameter of instr
- [IDE Plugin] Fix daemon bloat and UI thread stalling for IDE plugin (#1916)
- [IDE Plugin] Handle null module scenario (#1902)
- [IDE Plugin] In unconfigured sq files return empty string for the package name (#1920)
- [IDE Plugin] Fix grouped statements and add an integration test for them (#1820)
- [IDE Plugin] Use built in ModuleUtil to find the module for an element (#1854)
- [IDE Plugin] Only add valid elements to lookups (#1909)
- [IDE Plugin] Parent can be null (#1857)

## [1.4.2] - 2020-08-27
### Added
- [Runtime] Support new JS IR backend
- [Gradle Plugin] Add generateSqlDelightInterface Gradle task. (by [Niklas Baudy][vanniktech])
- [Gradle Plugin] Add verifySqlDelightMigration Gradle task. (by [Niklas Baudy][vanniktech])

### Fixed
- [IDE Plugin] Use the gradle tooling API to facilitate data sharing between the IDE and gradle
- [IDE Plugin] Default to false for schema derivation
- [IDE Plugin] Properly retrieve the commonMain source set
- [MySQL Dialect] Added minute to mySqlFunctionType() (by [MaaxGr][maaxgr])

## [1.4.1] - 2020-08-21
### Added
- [Runtime] Support Kotlin 1.4.0 (#1859)

### Changed
- [Gradle Plugin] Make AGP dependency compileOnly (#1362)

### Fixed
- [Compiler] Add optional javadoc to column defintion rule and to table interface generator (#1224 by [Daniel Eke][endanke])
- [SQLite Dialect] Add support for sqlite fts5 auxiliary functions highlight, snippet, and bm25 (by [Daniel Rampelt][drampelt])
- [MySQL Dialect] Support MySQL bit data type
- [MySQL Dialect] Support MySQL binary literals
- [PostgreSQL Dialect] Expose SERIAL from sql-psi (by [Veyndan Stuart][VeyndanS])
- [PostgreSQL Dialect] Add BOOLEAN data type (by [Veyndan Stuart][VeyndanS])
- [PostgreSQL Dialect] Add NULL column constraint (by [Veyndan Stuart][VeyndanS])
- [HSQL Dialect] Adds `AUTO_INCREMENT` support to HSQL (by [Ryan Harter][rharter])

## [1.4.0] - 2020-06-22
### Added
- [MySQL Dialect] MySQL support (by [Jeff Gulbronson][JeffG] & [Veyndan Stuart][VeyndanS])
- [PostgreSQL Dialect] Experimental PostgreSQL support (by [Veyndan Stuart][VeyndanS])
- [HSQL Dialect] Experimental H2 support (by [Marius Volkhart][MariusV])
- [SQLite Dialect] SQLite FTS5 support (by [Ben Asher][BenA] & [James Palawaga][JamesP])
- [SQLite Dialect] Support alter table rename column (#1505 by [Angus Holder][AngusH])
- [IDE] IDE support for migration (.sqm) files
- [IDE] Add SQLDelight Live Templates that mimic built-in SQL Live Templates (#1154 by [Veyndan Stuart][VeyndanS])
- [IDE] Add new SqlDelight file action (#42 by [Roman Zavarnitsyn][RomanZ])
- [Runtime] transactionWithReturn API for transactions that return results
- [Compiler] Syntax for grouping multiple SQL statements together in a .sq file
- [Compiler] Support generating schemas from migration files
- [Gradle Plugin] Add a task for outputting migration files as valid sql

### Changed
- [Documentation] Overhaul of the documentation website (by [Saket Narayan][SaketN])
- [Gradle Plugin] Improve unsupported dialect error message (by [Veyndan Stuart][VeyndanS])
- [IDE] Dynamically change file icon based on dialect (by [Veyndan Stuart][VeyndanS])
- [JDBC Driver] Expose a JdbcDriver constructor off of javax.sql.DataSource (#1614)

### Fixed
- [Compiler]Support Javadoc on tables and fix multiple javadoc in one file (#1224)
- [Compiler] Enable inserting a value for synthesized columns (#1351)
- [Compiler] Fix inconsistency in directory name sanitizing (by [Zac Sweers][ZacSweers])
- [Compiler] Synthesized columns should retain nullability across joins (#1656)
- [Compiler] Pin the delete statement on the delete keyword (#1643)
- [Compiler] Fix quoting (#1525 by [Angus Holder][AngusH])
- [Compiler] Fix the between operator to properly recurse into expressions (#1279)
- [Compiler] Give better error for missing table/column when creating an index (#1372)
- [Compiler] Enable using the outer querys projection in join constraints (#1346)
- [Native Driver] Make execute use transationPool (by [Ben Asher][BenA])
- [JDBC Driver] Use the jdbc transaction APIs instead of sqlite (#1693)
- [IDE] Fix virtualFile references to always be the original file (#1782)
- [IDE] Use the correct throwable when reporting errors to bugsnag (#1262)
- [Paging Extension] Fix leaky DataSource (#1628)
- [Gradle Plugin] If the output db file already exists when generating a schema, delete it (#1645)
- [Gradle Plugin] Fail migration validation if there are gaps
- [Gradle Plugin] Explicitely use the file index we set (#1644)

## [1.3.0] - 2020-04-03

* New: [Gradle] Dialect property to specify with sql dialect to compile against.
* New: [Compiler] #1009 Experimental support of the mysql dialect.
* New: [Compiler] #1436 Support of sqlite:3.24 dialect and upsert.
* New: [JDBC Driver] Split out JDBC driver from sqlite jvm driver.
* Fix: [Compiler] #1199 Support lambdas of any length.
* Fix: [Compiler] #1610 Fix the return type of avg() to be nullable.
* Fix: [IntelliJ] #1594 Fix path separator handling which broke Goto and Find Usages on Windows.

## [1.2.2] - 2020-01-22

* New: [Runtime] Support for Windows (mingW), tvOS, watchOS, and macOS architectures.
* Fix: [Compiler] Return type of sum() should be nullable.
* Fix: [Paging] Pass Transacter into QueryDataSourceFactory to avoid race conditions.
* Fix: [IntelliJ Plugin] Don't search through dependencies when looking for a file's package name.
* Fix: [Gradle] #862 Change validator logs in Gradle to debug level.
* Enhancement: [Gradle] Convert GenerateSchemaTask to use Gradle worker.
* Note: sqldelight-runtime artifact renamed to runtime.


## [1.2.1] - 2019-12-11

* Fix: [Gradle] Kotlin Native 1.3.60 support.
* Fix: [Gradle] #1287 Warning when syncing.
* Fix: [Compiler] #1469 SynetheticAccessor creation for query.
* Fix: [JVM Driver] Fixed memory leak.
* NOTE: The coroutine extension artifact requires kotlinx bintray maven repository be added to your buildscript.

## [1.2.0] - 2019-08-30

* New: [Runtime] Stable Flow api.
* Fix: [Gradle] Kotlin Native 1.3.50 support.
* Fix: [Gradle] #1380 Clean build sometimes fails.
* Fix: [Gradle] #1348 Running verify tasks prints "Could not retrieve functions".
* Fix: [Compile] #1405 Can't build project if query contains FTS table joined.
* Fix: [Gradle] #1266 Sporadic gradle build failure while having multiple database modules.

## [1.1.4] - 2019-07-11

* New: [Runtime] Experimental kotlin Flow api.
* Fix: [Gradle] Kotlin/Native 1.3.40 compatibility.
* Fix: [Gradle] #1243 Fix for usage of SQLDelight with Gradle configure on demand.
* Fix: [Gradle] #1385 Fix for usage of SQLDelight with incremental annotation processing.
* Fix: [Gradle] Allow gradle tasks to cache.
* Fix: [Gradle] #1274 Enable usage of sqldelight extension with kotlin dsl.
* Fix: [Compiler] Unique ids are generated for each query deterministically.
* Fix: [Compiler] Only notify listening queries when a transaction is complete.
* Fix: [JVM Driver] #1370 Force JdbcSqliteDriver users to supply a DB URL.

## [1.1.3] - 2019-04-14

* Gradle Metadata 1.0 release.

## [1.1.2] - 2019-04-14

* New: [Runtime] #1267 Logging driver decorator.
* Fix: [Compiler] #1254 Split string literals which are longer than 2^16 characters.
* Fix: [Gradle] #1260 generated sources are recognized as iOS source in Multiplatform Project.
* Fix: [IDE] #1290 kotlin.KotlinNullPointerException in CopyAsSqliteAction.kt:43.
* Fix: [Gradle] #1268 Running linkDebugFrameworkIos* tasks fail in recent versions.

## [1.1.1] - 2019-03-01

* Fix: [Gradle] Fix module dependency compilation for android projects.
* Fix: [Gradle] #1246 Set up api dependencies in afterEvaluate.
* Fix: [Compiler] Array types are properly printed.

## [1.1.0] - 2019-02-27

* New: [Gradle] #502 Allow specifying schema module dependencies.
* Enhancement: [Compiler] #1111 Table errors are sorted before other errors.
* Fix: [Compiler] #1225 Return the correct type for REAL literals.
* Fix: [Compiler] #1218 docid propagates through triggers.

## [1.0.3] - 2019-01-30

* Enhancement: [Runtime] #1195 Native Driver/Runtime Arm32.
* Enhancement: [Runtime] #1190 Expose the mapper from the Query type.

## [1.0.2] - 2019-01-26

* Fix: [Gradle Plugin] Update to kotlin 1.3.20.
* Fix: [Runtime] Transactions no longer swallow exceptions.

## [1.0.1] - 2019-01-21

* Enhancement: [Native Driver] Allow passing directory name to DatabaseConfiguration.
* Enhancement: [Compiler] #1173 Files without a package fail compilation.
* Fix: [IDE] Properly report IDE errors to Square.
* Fix: [IDE] #1162 Types in the same package show as error but work fine.
* Fix: [IDE] #1166 Renaming a table fails with NPE.
* Fix: [Compiler] #1167 Throws an exception when trying to parse complex SQL statements with UNION and SELECT.

## [1.0.0] - 2019-01-08

* New: Complete overhaul of generated code, now in kotlin.
* New: RxJava2 extensions artifact.
* New: Android Paging extensions artifact.
* New: Kotlin Multiplatform support.
* New: Android, iOS and JVM SQLite driver artifacts.
* New: Transaction API.

## [0.7.0] - 2018-02-12

 * New: Generated code has been updated to use the Support SQLite library only. All queries now generate statement objects instead of a raw strings.
 * New: Statement folding in the IDE.
 * New: Boolean types are now automatically handled.
 * Fix: Remove deprecated marshals from code generation.
 * Fix: Correct 'avg' SQL function type mapping to be REAL.
 * Fix: Correctly detect 'julianday' SQL function.


## [0.6.1] - 2017-03-22

 * New: Delete Update and Insert statements without arguments get compiled statements generated.
 * Fix: Using clause within a view used in a subquery doesn't error.
 * Fix: Duplicate types on generated Mapper removed.
 * Fix: Subqueries can be used in expressions that check against arguments.

## [0.6.0] - 2017-03-06

 * New: Select queries are now exposed as a `SqlDelightStatement` factory instead of string constants.
 * New: Query JavaDoc is now copied to statement and mapper factories.
 * New: Emit string constants for view names.
 * Fix: Queries on views which require factories now correctly require those factories are arguments.
 * Fix: Validate the number of arguments to an insert matches the number of columns specified.
 * Fix: Properly encode blob literals used in where clauses.
 * Gradle 3.3 or newer is required for this release.

## [0.5.1] - 2016-10-24

 * New: Compiled statements extend an abstract type.
 * Fix: Primitive types in parameters will be boxed if nullable.
 * Fix: All required factories for bind args are present in factory method.
 * Fix: Escaped column names are marshalled correctly.

## [0.5.0] - 2016-10-19

 * New: SQLite arguments can be passed typesafely through the Factory
 * New: IntelliJ plugin performs formatting on .sq files
 * New: Support for SQLite timestamp literals
 * Fix: Parameterized types can be clicked through in IntelliJ
 * Fix: Escaped column names no longer throw RuntimeExceptions if grabbed from Cursor.
 * Fix: Gradle plugin doesn't crash trying to print exceptions.

## [0.4.4] - 2016-07-20

 * New: Native support for shorts as column java type
 * New: Javadoc on generated mappers and factory methods
 * Fix: group_concat and nullif functions have proper nullability
 * Fix: Compatibility with Android Studio 2.2-alpha
 * Fix: WITH RECURSIVE no longer crashes plugin

## [0.4.3] - 2016-07-07

 * New: Compilation errors link to source file.
 * New: Right-click to copy SQLDelight code as valid SQLite.
 * New: Javadoc on named statements will appear on generated Strings.
 * Fix: Generated view models include nullability annotations.
 * Fix: Generated code from unions has proper type and nullability to support all possible columns.
 * Fix: sum and round SQLite functions have proper type in generated code.
 * Fix: CAST's, inner selects bugfixes.
 * Fix: Autocomplete in CREATE TABLE statements.
 * Fix: SQLite keywords can be used in packages.

## [0.4.2] - 2016-06-16

 * New: Marshal can be created from the factory.
 * Fix: IntelliJ plugin generates factory methods with proper generic order.
 * Fix: Function names can use any casing.

## [0.4.1] - 2016-06-14

 * Fix: IntelliJ plugin generates classes with proper generic order.
 * Fix: Column definitions can use any casing.

## [0.4.0] - 2016-06-14

 * New: Mappers are generated per query instead of per table.
 * New: Java types can be imported in .sq files.
 * New: SQLite functions are validated.
 * Fix: Remove duplicate errors.
 * Fix: Uppercase column names and java keyword column names do not error.

## [0.3.2] - 2016-05-14

 * New: Autocompletion and find usages now work for views and aliases.
 * Fix: Compile-time validation now allows functions to be used in selects.
 * Fix: Support insert statements which only declare default values.
 * Fix: Plugin no longer crashes when a project not using SQLDelight is imported.


## [0.3.1] - 2016-04-27

  * Fix: Interface visibility changed back to public to avoid Illegal Access runtime exceptions from method references.
  * Fix: Subexpressions are evaluated properly.


## [0.3.0] - 2016-04-26

  * New: Column definitions use SQLite types and can have additional 'AS' constraint to specify java type.
  * New: Bug reports can be sent from the IDE.
  * Fix: Autocomplete functions properly.
  * Fix: SQLDelight model files update on .sq file edit.
  * Removed: Attached databases no longer supported.


## [0.2.2] - 2016-03-07

 * New: Compile-time validation of the columns used by insert, update, delete, index, and trigger statements.
 * Fix: Don't crash IDE plugin on file move/create.


## [0.2.1] - 2016-03-07

 * New: Ctrl+`/` (Cmd+`/` on OSX) toggles comment of the selected line(s).
 * New: Compile-time validation of the columns used by SQL queries.
 * Fix: Support Windows paths in both the IDE and Gradle plugin.


## [0.2.0] - 2016-02-29

 * New: Added copy constructor to Marshal class.
 * New: Update to Kotlin 1.0 final.
 * Fix: Report 'sqldelight' folder structure problems in a non-failing way.
 * Fix: Forbid columns named `table_name`. Their generated constant clashes with the table name constant.
 * Fix: Ensure IDE plugin generates model classes immediately and regardless of whether `.sq` files were opened.
 * Fix: Support Windows paths in both the IDE and Gradle plugin.


## [0.1.2] - 2016-02-13

 * Fix: Remove code which prevented the Gradle plugin from being used in most projects.
 * Fix: Add missing compiler dependency on the Antlr runtime.


## [0.1.1] - 2016-02-12

 * Fix: Ensure the Gradle plugin points to the same version of the runtime as itself.


## [0.1.0] - 2016-02-12

Initial release.

  [JeffG]: https://github.com/JGulbronson
  [VeyndanS]: https://github.com/veyndan
  [BenA]: https://github.com/benasher44
  [JamesP]: https://github.com/jpalawaga
  [MariusV]: https://github.com/MariusVolkhart
  [SaketN]: https://github.com/saket
  [RomanZ]: https://github.com/romtsn
  [ZacSweers]: https://github.com/ZacSweers
  [AngusH]: https://github.com/angusholder
  [drampelt]: https://github.com/drampelt
  [endanke]: https://github.com/endanke
  [rharter]: https://github.com/rharter
  [vanniktech]: https://github.com/vanniktech
  [maaxgr]: https://github.com/maaxgr
  [eygraber]: https://github.com/eygraber
  [lawkai]: https://github.com/lawkai
  [felipecsl]: https://github.com/felipecsl
  [dellisd]: https://github.com/dellisd
  [stephanenicolas]: https://github.com/stephanenicolas
  [oldergod]: https://github.com/oldergod
  [qjroberts]: https://github.com/qjroberts
  [kevincianfarini]: https://github.com/kevincianfarini
  [andersio]: https://github.com/andersio
  [ilmat192]: https://github.com/ilmat192
  [3flex]: https://github.com/3flex
  [aperfilyev]: https://github.com/aperfilyev
  [satook]: https://github.com/Satook
  [thomascjy]: https://github.com/ThomasCJY
  [pyricau]: https://github.com/pyricau
  [hannesstruss]: https://github.com/hannesstruss
  [martinbonnin]: https://github.com/martinbonnin
  [enginegl]: https://github.com/enginegl
  [pchmielowski]: https://github.com/pchmielowski
  [chippmann]: https://github.com/chippmann
  [IliasRedissi]: https://github.com/IliasRedissi
  [ahmedre]: https://github.com/ahmedre
  [pabl0rg]: https://github.com/pabl0rg
  [hfhbd]: https://github.com/hfhbd
  [sdoward]: https://github.com/sdoward
  [PhilipDukhov]: https://github.com/PhilipDukhov
  [julioromano]: https://github.com/julioromano
  [PaulWoitaschek]: https://github.com/PaulWoitaschek
  [kpgalligan]: https://github.com/kpgalligan
  [robx]: https://github.com/robxyy
  [madisp]: https://github.com/madisp
  [svenjacobs]: https://github.com/svenjacobs
  [jeffdgr8]: https://github.com/jeffdgr8
  [bellatoris]: https://github.com/bellatoris
  [sachera]: https://github.com/sachera
  [sproctor]: https://github.com/sproctor
  [davidwheeler123]: https://github.com/davidwheeler123
  [C2H6O]: https://github.com/C2H6O
  [griffio]: https://github.com/griffio
  [shellderp]: https://github.com/shellderp
  [joshfriend]: https://github.com/joshfriend
  [daio]: https://github.com/daio
  [morki]: https://github.com/morki
  [Adriel-M]: https://github.com/Adriel-M
  [05nelsonm]: https://github.com/05nelsonm
  [jingwei99]: https://github.com/jingwei99
  [anddani]: https://github.com/anddani
  [BoD]: https://github.com/BoD
  [de-luca]: https://github.com/de-luca
  [MohamadJaara]: https://github.com/MohamadJaara
  [nwagu]: https://github.com/nwagu
