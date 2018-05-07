package com.squareup.sqlite.migrations

import de.danielbechler.diff.ObjectDifferBuilder
import de.danielbechler.diff.node.DiffNode

object ObjectDifferDatabaseComparator : DatabaseComparator<CatalogDatabase> {

  override fun compare(db1: CatalogDatabase, db2: CatalogDatabase): DatabaseDiff {
    return ObjectDifferDatabaseDiff(differBuilder().compare(db1.catalog, db2.catalog))
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
      // Definition changes aren't important, its just things like comments or whitespace.
      propertyName("definition")
    }
    // Partial columns are used for unresolved columns to avoid cycles. Matching based on string
    // is fine for our purposes.
    comparison().ofType(Class.forName("schemacrawler.crawl.ColumnPartial")).toUseEqualsMethod()
  }.build()
}
