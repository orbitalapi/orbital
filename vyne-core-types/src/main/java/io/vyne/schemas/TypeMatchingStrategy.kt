package io.vyne.schemas

import io.vyne.utils.synonymFullQualifiedName
import lang.taxi.types.EnumType

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
   },
   ALLOW_SYNONYMS {
      override fun matches(requestedType: Type, candidate: Type): Boolean {
        if (!requestedType.isEnum || !candidate.isEnum) {
           return false
        }
         val underlyingEnumType = candidate.taxiType as EnumType
         underlyingEnumType
            .values.flatMap { underlyingEnumType -> underlyingEnumType.synonyms }
            .find {  enumQualifiedName -> enumQualifiedName.synonymFullQualifiedName() == requestedType.fullyQualifiedName }
            ?.let {
               return true
            }
         return false
      }

   };


   abstract fun matches(requestedType: Type, candidate: Type): Boolean

}
