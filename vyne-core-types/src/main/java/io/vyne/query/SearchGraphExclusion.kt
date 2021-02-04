package io.vyne.query

/**
 * Excludes a specifc thing from the search.
 * Wraps with a reason - useful for debugging only.
 */
data class SearchGraphExclusion<T>(val reason: String, val excludedValue: T)

fun <T> Iterable<SearchGraphExclusion<T>>.excludedValues(): Set<T> {
   return this.map { it.excludedValue }.toSet()
}

