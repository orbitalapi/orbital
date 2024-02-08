package com.orbitalhq.models

import com.orbitalhq.schemas.Type
import lang.taxi.types.PrimitiveType

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
}
