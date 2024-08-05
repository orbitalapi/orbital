package com.orbitalhq.models

import com.orbitalhq.schemas.Type
import lang.taxi.types.PrimitiveType
import lang.taxi.utils.TypeUtils

object TypeUtils {
   /**
    * If the reciever is Any, then will use the valueType
    */
   fun upcastIfPossible(valueType: Type, receiverType: Type):Type {
      // In future, we could expand this to select the most specific type
      return when {
         receiverType.taxiType == PrimitiveType.ANY -> valueType
         else -> receiverType
      }
   }
   fun mostSpecificType(typeA: lang.taxi.types.Type, typeB: lang.taxi.types.Type): lang.taxi.types.Type = TypeUtils.getMostSpecificType(typeA, typeB)
   fun mostSpecificType(typeA: Type, typeB: Type):Type {
      val mostSpecificTaxiType = TypeUtils.getMostSpecificType(typeA.taxiType, typeB.taxiType)
      return when {
         typeA.taxiType == mostSpecificTaxiType -> typeA
         typeB.taxiType == mostSpecificTaxiType -> typeB
         else -> error("Expected either of the two types provided (${typeA.paramaterizedName}, ${typeB.paramaterizedName}) would be the most specific type, but got ${mostSpecificTaxiType.qualifiedName}")
      }
   }
}
