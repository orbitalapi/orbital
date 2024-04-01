package com.orbitalhq.connectors.utils

import lang.taxi.types.FieldReference
import lang.taxi.types.ObjectType
import lang.taxi.types.Type

fun getSingleField(sourceType: ObjectType, fieldType: Type): FieldReference {
   val references = sourceType.fieldReferencesAssignableTo(fieldType)
   return when {
      references.isEmpty() -> error("Field ${fieldType.qualifiedName} is not present on type ${sourceType.qualifiedName}")
      references.size == 1 -> {
         val reference = references.single()
         if (reference.path.size == 1) {
            reference
         } else {
            error(
               "${fieldType.qualifiedName} is only accessible on ${sourceType} via a nested property (${
                  reference.path.joinToString(
                     "."
                  )
               }), which is not supported in SQL statements"
            )
         }
      }

      else -> error("Field ${fieldType.qualifiedName} is ambiguous on type ${sourceType.qualifiedName} - expected a single match, but found ${references.joinToString { it.description }}")
   }
}
