package io.vyne.cask.types

import io.vyne.schemas.VersionedType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType

fun VersionedType.allFields(): List<Field> {
   return when (taxiType) {
      is ObjectType -> (taxiType as ObjectType).allFields
      else -> TODO("Support for non-object types coming")
   }
}
