package app.cash.sqlite.migrations

import de.danielbechler.diff.node.DiffNode
import de.danielbechler.diff.node.DiffNode.State.CHANGED
import schemacrawler.schema.CrawlInfo
import schemacrawler.schema.JdbcDriverInfo

class ObjectDifferDatabaseDiff(
  private val diff: DiffNode,
  private val db1: CatalogDatabase,
  private val db2: CatalogDatabase
) : DatabaseDiff {

  override fun printTo(out: Appendable) = with(out) {
    diff.visit { node, visit ->
      if (CrawlInfo::class.java.isAssignableFrom(node.valueType) ||
        JdbcDriverInfo::class.java.isAssignableFrom(node.valueType)
      ) {
        visit.dontGoDeeper()
        return@visit
      }
      if (node.childCount() == 0) {
        append("${node.path} - ${node.state}\n")
        if (node.state == CHANGED) {
          append(
            """
            |  BEFORE:
            |${node.canonicalGet(db1.catalog).toString().prependIndent("    ")}
            |  AFTER:
            |${node.canonicalGet(db2.catalog).toString().prependIndent("    ")}
            |""".trimMargin()
          )
        }
      } else if (node.state == DiffNode.State.ADDED || node.state == DiffNode.State.REMOVED) {
        append("${node.path} - ${node.state}\n")
        visit.dontGoDeeper()
      }
    }
  }

  override fun toString(): String {
    val sb = StringBuilder()
    printTo(sb)
    return sb.toString()
  }
}
