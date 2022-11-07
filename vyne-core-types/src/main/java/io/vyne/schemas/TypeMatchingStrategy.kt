package io.vyne.schemas

interface TypeMatchingPredicate {
   fun matches(requestedType: Type, candidate: Type): Boolean
}

fun TypeMatchingPredicate.or(other: TypeMatchingPredicate): TypeMatchingPredicate {
   return CompositeOrPredicate(listOf(this, other))
}

class CompositeOrPredicate(val predicates: List<TypeMatchingPredicate>) : TypeMatchingPredicate {
   override fun matches(requestedType: Type, candidate: Type): Boolean =
      predicates.any { it.matches(requestedType, candidate) }

}

enum class TypeMatchingStrategy : TypeMatchingPredicate {
   ALLOW_INHERITED_TYPES {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         // I've flip-flopped here.
         // evaluating against the collection type breaks
         // searches for a collection.
         // What was the scenario we needed this?

//         return if (requestedType.isCollection) {
//            ALLOW_INHERITED_TYPES.matches(requestedType.collectionType!!, candidate)
//         } else {
         return requestedType.isAssignableFrom(candidate)
//         }
      }
   },

   /**
    * If the requested type is a collection,
    */
   MATCH_ON_COLLECTION_TYPE {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return if (requestedType.isCollection) {
            ALLOW_INHERITED_TYPES.matches(requestedType.collectionType!!, candidate)
         } else {
            ALLOW_INHERITED_TYPES.matches(requestedType, candidate)
         }
      }
   },
   EXACT_MATCH {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.fullyQualifiedName == candidate.fullyQualifiedName
      }
   };

}
