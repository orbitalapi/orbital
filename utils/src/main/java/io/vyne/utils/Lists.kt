package io.vyne.utils


/**
 * Operates on an immutable list
 * Returns a new list, containing the original members, with the others appended
 */
fun <T> List<T>.concat(vararg others: T): List<T> {
   val mutable = this.toMutableList()
   mutable.addAll(others)
   return mutable.toList()
}
