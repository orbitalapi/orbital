package com.orbitalhq.query

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.TaxiTypeMapper
import com.orbitalhq.schemas.Type


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
//      schema.typeCache.registerAnonymousType(vyneAnonymousType)
//      val retValue = schema.type(taxiType.toVyneQualifiedName())
//      (parameterType as ObjectType).fields.forEach { anonymoustTypeField ->
//         if (anonymoustTypeField.type.anonymous) {
//            toVyneAnonymousType(anonymoustTypeField.type, schema)
//         }
//      }
      return vyneAnonymousType
   }
}

