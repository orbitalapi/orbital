package io.vyne.query

import io.vyne.schemas.Schema
import io.vyne.schemas.TaxiTypeMapper
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneQualifiedName
import lang.taxi.types.ObjectType


object ProjectionAnonymousTypeProvider {
   fun projectedTo(taxiType: lang.taxi.types.Type, schema: Schema): Type {
      return when {
         taxiType.anonymous -> toVyneAnonymousType(taxiType, schema)
         else -> schema.type(taxiType)
      }

   }

   fun toVyneAnonymousType(taxiType: lang.taxi.types.Type, schema: Schema): Type {
      val parameterType = if (taxiType.typeParameters().isNotEmpty()) taxiType.typeParameters().first() else taxiType
      val vyneAnonymousType = TaxiTypeMapper.fromTaxiType(parameterType, schema)
      schema.typeCache.registerAnonymousType(vyneAnonymousType)
      val retValue = schema.type(taxiType.toVyneQualifiedName())
      (parameterType as ObjectType).fields.forEach { anonymoustTypeField ->
         if (anonymoustTypeField.type.anonymous || anonymoustTypeField.type.formattedInstanceOfType != null) {
            toVyneAnonymousType(anonymoustTypeField.type, schema)
         }
      }
      return retValue
   }
}

