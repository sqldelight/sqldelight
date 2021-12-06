package app.cash.sqlite.migrations

import de.danielbechler.diff.node.DiffNode
import schemacrawler.schema.CrawlInfo
import schemacrawler.schema.JdbcDriverInfo

class ObjectDifferDatabaseDiff(
  private val diff: DiffNode
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
