package app.cash.sqldelight.db

/**
 * Thrown when the underlying driver detects that the current cursor row
 * is no longer backed by the active platform cursor window (e.g.
 * Android's CursorWindow) due to concurrent mutations.
 *
 * This indicates that the row cannot be read reliably.
 */
class StaleWindowException(message: String) : RuntimeException(message)
