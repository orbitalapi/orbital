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

   fun toVyneStreamOfAnonymousType(taxiType: lang.taxi.types.Type, schema: Schema): Type {
      val cache = schema.typeCache.copy()

      taxiType.typeParameters().map {
         cache.add(TaxiTypeMapper.fromTaxiType(it, schema, cache))
      }

      return TaxiTypeMapper.fromTaxiType(taxiType, schema, cache)
   }
   fun toVyneAnonymousType(taxiType: lang.taxi.types.Type, schema: Schema): Type {
      // MP 22-Jan-24: This used to unwrap array types, so the
      // return value of T[] was T.
      // That was to handle cases where we're projecting
      // find { Foo[] } as { ... }
      // The logic has been encapsulated in LocalProjectionProvider,
      // where we correctly handle multiple edge cases around
      // Source and Target types.
      return TaxiTypeMapper.fromTaxiType(taxiType, schema)
   }
}

