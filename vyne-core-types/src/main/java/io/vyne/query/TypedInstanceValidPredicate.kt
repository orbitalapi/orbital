package io.vyne.query

import io.vyne.models.TypedInstance
import io.vyne.schemas.Operation

/**
 * Interface to provide fine-grained control over
 * building of instances and determining if proposed instances
 * are suitable.
 *
 * Current use case is here to support the concept of annotations on target
 * types for things like FirstNotEmpty, without littering code everywhere.
 *
 * This is a sketch, and may not live long.
 *
 */
interface TypedInstanceValidPredicate {
   fun isValid(typedInstance:TypedInstance?):Boolean
}

/**
 * A spec that will accept anything.
 * This serves as the default.
 */
object AlwaysGoodSpec : TypedInstanceValidPredicate {
   override fun isValid(typedInstance: TypedInstance?) = true
}

data class InvocationConstraints(val typedInstanceValidPredicate: TypedInstanceValidPredicate, val excludedOperations: Set<Operation> = emptySet()) {
   companion object  {
     val  withAlwaysGoodPredicate: InvocationConstraints = InvocationConstraints(AlwaysGoodSpec)
   }
}
