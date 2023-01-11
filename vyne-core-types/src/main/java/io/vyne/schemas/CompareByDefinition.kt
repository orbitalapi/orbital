package io.vyne.schemas

/**
 * Indicates the class has a way other than == for
 * determining if two instances are defined the same.
 *
 * Used when we want a fast == (eg., for hash codes),
 * but it's insufficient to determine true deep equality.
 *
 */
interface CompareByDefinition<T> {
   fun isDefinedSameAs(other: T): Boolean
}
