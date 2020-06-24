package io.vyne.schemas

enum class TypeMatchingStrategy {
   ALLOW_INHERITED_TYPES {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.isAssignableFrom(candidate)
//         return requestedType.fullyQualifiedName == candidate.fullyQualifiedName ||
//            candidate.inheritanceGraph.map { it.fullyQualifiedName }.contains(requestedType.fullyQualifiedName)
      }
   },
   EXACT_MATCH {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
         return requestedType.fullyQualifiedName == candidate.fullyQualifiedName
      }
   };


   abstract fun matches(requestedType: Type, candidate: Type): Boolean

}
