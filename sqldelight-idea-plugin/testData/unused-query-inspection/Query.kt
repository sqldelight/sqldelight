class Query<T>(val value: T) {
  fun executeAsOneOrNull(): T {
    return value
  }
}
