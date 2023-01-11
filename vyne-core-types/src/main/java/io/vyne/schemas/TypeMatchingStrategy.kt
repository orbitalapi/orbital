package io.vyne.schemas

interface TypeMatchingPredicate {
   val id: String
   fun matches(requestedType: Type, candidate: Type): Boolean
}

fun TypeMatchingPredicate.or(other: TypeMatchingPredicate): TypeMatchingPredicate {
   return CompositeOrPredicate(listOf(this, other))
}

class CompositeOrPredicate(val predicates: List<TypeMatchingPredicate>) : TypeMatchingPredicate {
   override fun matches(requestedType: Type, candidate: Type): Boolean =
      predicates.any { it.matches(requestedType, candidate) }

   override val id: String = predicates.joinToString(" OR ") { it.id }
}

enum class TypeMatchingStrategy : TypeMatchingPredicate {
   ALLOW_INHERITED_TYPES {
      override val id: String = "ALLOW_INHERITED_TYPES"
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.isAssignableFrom(candidate)
      }
   },

   /**.
    * If the requested type is a collection, match if the candidate is the member type
    * i.e., Match when requestedType = T[] and candidate = T
    */
   MATCH_ON_COLLECTION_TYPE {
      override val id: String = "MATCH_ON_COLLECTION_TYPE"
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return if (requestedType.isCollection) {
            ALLOW_INHERITED_TYPES.matches(requestedType.collectionType!!, candidate)
         } else {
            ALLOW_INHERITED_TYPES.matches(requestedType, candidate)
         }
      }
   },
   /**
    * If the candidate is a collection, match if the requested type is the member.
    * ie., Match when requestedType == T and candidate == T[]
    */
   MATCH_ON_COLLECTION_OF_TYPE {
      override val id: String = "MATCH_ON_COLLECTION_MEMBERS"
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return if (candidate.isCollection && !requestedType.isCollection) {
            ALLOW_INHERITED_TYPES.matches(requestedType, candidate.collectionType!!)
         } else {
            ALLOW_INHERITED_TYPES.matches(requestedType, candidate)
         }
      }
   },
   EXACT_MATCH {
      override val id: String = "EXACT_MATCH"
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.fullyQualifiedName == candidate.fullyQualifiedName
      }
   };

}
