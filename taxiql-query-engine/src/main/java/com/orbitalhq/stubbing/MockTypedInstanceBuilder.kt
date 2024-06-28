package com.orbitalhq.stubbing

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.EnumType
import lang.taxi.types.PrimitiveType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object MockTypedInstanceBuilder {

   fun build(type: Type, schema: Schema):TypedInstance {
      return when {
         type.isStream -> buildStream(type, schema)
         type.isScalar -> buildTypedValue(type, schema)
         type.attributes.isNotEmpty() -> buildTypedObject(type, schema)
         type.isCollection -> buildTypedCollection(type, schema)
         else -> TODO("Can't build a stubbed typed instance for type: ${type.name.longDisplayName}")
      }
   }

   private fun buildStream(type: Type, schema: Schema): TypedInstance {
      return build(type.collectionType!!, schema)
   }

   private fun buildTypedObject(type: Type, schema: Schema): TypedInstance {
      val values = type.attributes
         // Exclude accessors, so they get evaluated during build time
         .filter { (a,b) -> b.accessor == null }
         .map { (name, field) ->
         val attributeValue = build(field.resolveType(schema), schema)
         name to attributeValue
      }.toMap()
      return TypedInstance.from(type, values, source = UndefinedSource, schema = schema)
   }

   private fun buildTypedCollection(type: Type, schema: Schema): TypedInstance {
      val memberType = type.collectionType!!
      val member = build(memberType, schema)
      return TypedCollection.from(listOf(member))
   }

   private fun buildTypedValue(type: Type, schema: Schema): TypedInstance {
      return if (type.isEnum) {
         val enumValue = (type.taxiType as EnumType).values.first().value
         TypedInstance.from(type, enumValue, schema, source = UndefinedSource)
      } else {
         val value = when (type.taxiType.basePrimitive!!) {
            PrimitiveType.BOOLEAN -> true
            PrimitiveType.STRING -> "stub"
            PrimitiveType.INTEGER -> 100
            PrimitiveType.LONG -> 100L
            PrimitiveType.DECIMAL -> BigDecimal.valueOf(10)
            PrimitiveType.LOCAL_DATE -> LocalDate.now()
            PrimitiveType.TIME -> LocalTime.now()
            PrimitiveType.DATE_TIME -> ZonedDateTime.now()
            PrimitiveType.INSTANT -> Instant.now()
            PrimitiveType.ANY -> "any"
            PrimitiveType.DOUBLE -> 2.0
            PrimitiveType.VOID -> null
         }
         TypedInstance.from(type, value, schema, source = UndefinedSource)
      }
   }
}
