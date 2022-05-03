package app.cash.sqlite.migrations

import de.danielbechler.diff.ObjectDifferBuilder
import de.danielbechler.diff.comparison.ComparisonStrategy
import de.danielbechler.diff.node.DiffNode
import de.danielbechler.diff.node.DiffNode.State.ADDED
import de.danielbechler.diff.node.DiffNode.State.CHANGED
import de.danielbechler.diff.node.DiffNode.State.REMOVED
import de.danielbechler.diff.node.DiffNode.State.UNTOUCHED

class ObjectDifferDatabaseComparator(
  private val circularReferenceExceptionLogger: ((String) -> Unit)? = null
) : DatabaseComparator<CatalogDatabase> {

  override fun compare(db1: CatalogDatabase, db2: CatalogDatabase): DatabaseDiff {
    return ObjectDifferDatabaseDiff(
      differBuilder().compare(db1.catalog, db2.catalog),
      db1,
      db2
    )
  }

  private fun differBuilder() = ObjectDifferBuilder.startBuilding().apply {
    filtering().omitNodesWithState(DiffNode.State.UNTOUCHED)
    filtering().omitNodesWithState(DiffNode.State.CIRCULAR)
    inclusion().exclude().apply {
      propertyName("fullName")
      propertyName("parent")
      propertyName("exportedForeignKeys")
      propertyName("importedForeignKeys")
      propertyName("deferrable")
      propertyName("initiallyDeferred")
    }
    // Partial columns are used for unresolved columns to avoid cycles. Matching based on string
    // is fine for our purposes.
    comparison().ofType(Class.forName("schemacrawler.crawl.ColumnPartial")).toUseEqualsMethod()

    // This is only used to compare definitions. Definitions include whitespace and comments
    // so we want those removed.
    comparison().ofType(String::class.java).toUse(object : ComparisonStrategy {
      override fun compare(node: DiffNode, type: Class<*>, working: Any?, base: Any?) {
        if (working == null && base == null) {
          node.state = UNTOUCHED
        } else if (working == null || working !is String) {
          node.state = REMOVED
        } else if (base == null || base !is String) {
          node.state = ADDED
        } else if (working.normalizeDefinition()
          .equals(base.normalizeDefinition(), ignoreCase = true)
        ) {
          node.state = UNTOUCHED
        } else {
          node.state = CHANGED
        }
      }

      private fun String.normalizeDefinition() =
        replace(Regex("--(.*)"), "")
          .replace(Regex("[\\s\"]+"), "")
    })

    // Custom error handler for circular reference warnings which allows to override SL4J warning log
    val circularReferenceExceptionLogger = circularReferenceExceptionLogger
    if (circularReferenceExceptionLogger != null) {
      circularReferenceHandling().handleCircularReferenceExceptionsUsing { node ->
        // The same message as original CircularReferenceExceptionHandler
        val message = (
          "Detected circular reference in node at path ${node.path} " +
            "Going deeper would cause an infinite loop, so I'll stop looking at " +
            "this instance along the current path."
          )
        circularReferenceExceptionLogger(message)
      }
    }
  }.build()
}
