package io.vyne.query

import io.vyne.schemas.Schema
import io.vyne.schemas.TaxiTypeMapper
import io.vyne.schemas.Type
import io.vyne.schemas.taxi.toVyneQualifiedName
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.types.ObjectType
import lang.taxi.types.ProjectedType


object ProjectionAnonymousTypeProvider {
   fun projectedTo(projectedType: ProjectedType, schema: Schema): Type {
      return when {
         // Case for:
         // findAll { foo[] } as bar[]
         projectedType.concreteType != null && projectedType.anonymousTypeDefinition == null -> schema.type(projectedType.concreteType!!.toVyneQualifiedName())
         // Case for:
         // findAll { foo[] } as {
         // field1
         // field2
         // field3
         // field4: somenamespace.AnotherType
         // }[]
         projectedType.concreteType == null && projectedType.anonymousTypeDefinition != null -> {
            val anonymousTypeDefinition = projectedType.anonymousTypeDefinition!!
            vyneType(anonymousTypeDefinition, schema)
         }

         // Case for:
         // findAll { foo[] }
         // as bar[] {
         //    field1
         //    field2: mynamespace.mytype
         //}[]
         projectedType.concreteType != null && projectedType.anonymousTypeDefinition != null -> {
            val anonymousTypeDefinition = projectedType.anonymousTypeDefinition!!
            vyneType(anonymousTypeDefinition, schema)
         }

         else -> throw CompilationException(CompilationError(0, 0, "Invalid Anonymous Projection Type."))
      }
   }

   private fun vyneType(taxiType: lang.taxi.types.Type, schema: Schema): Type {
      val parameterType = if (taxiType.typeParameters().isNotEmpty()) taxiType.typeParameters().first() else taxiType
      val vyneAnonymousType = TaxiTypeMapper.fromTaxiType(parameterType, schema)
      schema.typeCache.registerAnonymousType(vyneAnonymousType)
      val retValue = schema.type(taxiType.toVyneQualifiedName())
      (parameterType as ObjectType).fields.forEach { anonymoustTypeField ->
         if (anonymoustTypeField.type.anonymous) {
            vyneType(anonymoustTypeField.type, schema)
         }
      }
      return retValue
   }
}

