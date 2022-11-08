package io.vyne.models

import io.vyne.query.AlwaysGoodSpec
import io.vyne.query.TypedInstanceValidPredicate
import io.vyne.schemas.Field

class TypedInstancePredicateFactory(private val predicateProviders: List<TypedInstancePredicateProvider> = TypedInstancePredicateProvider.providers) :
   TypedInstancePredicateProvider {
   override fun provide(field: Field): TypedInstanceValidPredicate {
      return predicateProviders
         .asSequence()
         .mapNotNull { it.provide(field) }
         .firstOrNull() ?: AlwaysGoodSpec

   }
}

interface TypedInstancePredicateProvider {
   // Note: I think we'll need to add methods for building
   // against various inputs here,
   // such as TypedInstance, Field, etc.
   // Not over-cooking this right now, as don't really know all the future
   // use cases
   fun provide(field: Field): TypedInstanceValidPredicate?

   companion object {
      val providers = listOf<TypedInstancePredicateProvider>(
         FirstNotEmptyPredicate
      )
   }
}
