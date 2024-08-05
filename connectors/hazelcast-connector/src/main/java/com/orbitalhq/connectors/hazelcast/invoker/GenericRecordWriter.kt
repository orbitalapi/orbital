package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.nio.serialization.FieldType
import com.hazelcast.nio.serialization.genericrecord.GenericRecord
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.containsMetadata
import com.orbitalhq.schemas.Schema
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.Arrays
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

object GenericRecordWriter {
   /**
    * Defines the serialization formats supported in Hazelcast.
    *
    * - `Compact`: Focuses on high performance and efficiency in serialization and deserialization processes.
    *               Designed for applications prioritizing speed and minimal memory footprint. Supports schema evolution
    *               with less overhead compared to Portable.
    *
    *  - 'Json' :  Values are returned as a HazelcastJson
    */
   enum class Format {
      Compact
   }

   private val logger = KotlinLogging.logger {}

   fun isWritable(typedInstance: TypedInstance): Boolean {
      return typedInstance.containsMetadata(HazelcastTaxi.Annotations.CompactObject.fullyQualifiedName)
   }

   fun getGenericRecordAndKey(typedInstance: TypedObject, schema: Schema):Pair<Any,GenericRecord> {
      val type = typedInstance.type
      val format = getHazelcastFormat(typedInstance.type)

      val genericRecord = buildGenericRecord(typedInstance, schema, format)
      val (keyField) = findKeyField(type)
      val key = typedInstance[keyField].toRawObject() ?: error("Field $keyField is the @Id, but is null or not present")
      return key to genericRecord
   }



   private fun getBuilder(typedInstance: TypedObject, schema: Schema, format: Format): GenericRecordBuilder {
      return when (format) {
         Format.Compact -> GenericRecordBuilder.compact(typedInstance.type.paramaterizedName)
      }

   }

   private fun buildGenericRecord(typedInstance: TypedObject, schema: Schema, format: Format): GenericRecord {
      val builder = getBuilder(typedInstance, schema, format)
      typedInstance.type.attributes.forEach { (fieldName, field) ->
         val fieldValue = typedInstance[fieldName]

         val rawFieldValue = fieldValue.toRawObject()?.let { rawFieldValue ->
            // for collection methods, we need an array, not a list.
            if (rawFieldValue is List<*>) {
               val collectionType = fieldValue.type.collectionType!!

               // At this point, return either the list of primitives (which get serialized
//               // using the Hazelcast method), or the TypedCollection,
//               // which gets recursed into, then ultimately converted into an array
//               // of portable objects.
               if (collectionType.isScalar) {
                  val jvmType = PrimitiveTypes.getJavaType(schema.type(collectionType.basePrimitiveTypeName!!).taxiType as PrimitiveType)
                  rawFieldValue.toTypedArray(jvmType)
               } else {
                  rawFieldValue
               }
            } else rawFieldValue
         }
         val hazelcastFieldType = getHazelcastFieldType(schema.type(field.type).taxiType, format)
         when (hazelcastFieldType) {
            null -> logger.warn { "Cannot serialize field $fieldName with type ${field.type.parameterizedName} as no serialization strategy was found" }
            FieldType.PORTABLE -> {
               require(fieldValue is TypedObject) { "Cannot serialize field $fieldName as value is ${fieldValue::class.simpleName} - for type ${field.type.parameterizedName} - expected a TypedObject" }
               val genericRecord = buildGenericRecord(fieldValue, schema, format)
               builder.setGenericRecord(fieldName, genericRecord)
            }

            FieldType.BOOLEAN -> builder.setNullableBoolean(fieldName, rawFieldValue as Boolean?)
            FieldType.CHAR -> builder.setChar(fieldName, rawFieldValue as Char)
            FieldType.INT -> builder.setNullableInt32(fieldName, rawFieldValue as Int?)
            FieldType.LONG -> builder.setNullableInt64(fieldName, rawFieldValue as Long?)
            FieldType.DOUBLE -> builder.setNullableFloat64(fieldName, rawFieldValue as Double?)
            FieldType.UTF -> builder.setString(fieldName, rawFieldValue as String?)
            FieldType.PORTABLE_ARRAY -> {
               require(fieldValue is TypedCollection) { "Cannot serialize field $fieldName as value is ${fieldValue::class.simpleName} - for type ${field.type.parameterizedName} - expected a TypedCollection" }
               val collection = fieldValue.map { member ->
                  require(member is TypedObject) { "Cannot serialize member of $fieldName as value is ${member::class.simpleName} - for type ${field.type.parameterizedName} - expected a TypedObject" }
                  buildGenericRecord(member, schema, format)
               }
               builder.setArrayOfGenericRecord(fieldName, collection.toTypedArray())
            }

            FieldType.BOOLEAN_ARRAY -> builder.setArrayOfNullableBoolean(fieldName, rawFieldValue as Array<out Boolean>)
            FieldType.INT_ARRAY -> builder.setArrayOfNullableInt32(fieldName, rawFieldValue as Array<out Int>?)
            FieldType.LONG_ARRAY -> builder.setArrayOfNullableInt64(fieldName, rawFieldValue as Array<out Long>?)
            FieldType.DOUBLE_ARRAY -> builder.setArrayOfNullableFloat64(fieldName, rawFieldValue as Array<out Double>?)
            FieldType.UTF_ARRAY -> builder.setArrayOfString(fieldName, rawFieldValue as Array<out String>?)

            FieldType.DECIMAL -> builder.setDecimal(fieldName, rawFieldValue as BigDecimal?)
            FieldType.DECIMAL_ARRAY -> builder.setArrayOfDecimal(fieldName, rawFieldValue as Array<out BigDecimal>?)
            FieldType.TIME -> builder.setTime(fieldName, rawFieldValue as LocalTime?)
            FieldType.TIME_ARRAY -> builder.setArrayOfTime(fieldName, rawFieldValue as Array<out LocalTime>?)
            FieldType.DATE -> builder.setDate(fieldName, rawFieldValue as LocalDate?)
            FieldType.DATE_ARRAY -> builder.setArrayOfDate(fieldName, rawFieldValue as Array<out LocalDate>?)
            FieldType.TIMESTAMP -> builder.setTimestamp(fieldName, rawFieldValue as LocalDateTime?)
            FieldType.TIMESTAMP_ARRAY -> builder.setArrayOfTimestamp(
               fieldName,
               rawFieldValue as Array<out LocalDateTime>?
            )

            FieldType.TIMESTAMP_WITH_TIMEZONE -> builder.setTimestampWithTimezone(
               fieldName,
               rawFieldValue as OffsetDateTime?
            )

            FieldType.TIMESTAMP_WITH_TIMEZONE_ARRAY -> builder.setArrayOfTimestampWithTimezone(
               fieldName,
               rawFieldValue as Array<out OffsetDateTime>?
            )

            else -> {
               logger.warn { "Cannot serialize with FieldType ${hazelcastFieldType.name} - no serialization strategy exists" }
            }
         }
      }
      return builder.build()
   }

   private fun getHazelcastFieldType(type: Type, format: Format): FieldType? {
      return when {
         type.isScalar -> {
            return when (type.basePrimitive!!) {
               PrimitiveType.BOOLEAN -> FieldType.BOOLEAN
               PrimitiveType.STRING -> FieldType.UTF
               PrimitiveType.INTEGER -> FieldType.INT
               PrimitiveType.LONG -> FieldType.LONG
               PrimitiveType.DECIMAL -> FieldType.DECIMAL
               PrimitiveType.LOCAL_DATE -> FieldType.DATE
               PrimitiveType.TIME -> FieldType.TIME
               PrimitiveType.DATE_TIME -> FieldType.TIMESTAMP
               PrimitiveType.INSTANT -> FieldType.TIMESTAMP_WITH_TIMEZONE
               PrimitiveType.ANY -> FieldType.CHAR
               PrimitiveType.DOUBLE -> FieldType.DOUBLE
               PrimitiveType.VOID -> null
               PrimitiveType.NOTHING -> null
            }
         }

         Arrays.isArray(type) -> {
            val memberType = type.typeParameters().first()
            if (memberType.isScalar) {
               when (memberType.basePrimitive!!) {
                  PrimitiveType.BOOLEAN -> FieldType.BOOLEAN_ARRAY
                  PrimitiveType.STRING -> FieldType.UTF_ARRAY
                  PrimitiveType.INTEGER -> FieldType.INT_ARRAY
                  PrimitiveType.LONG -> FieldType.LONG_ARRAY
                  PrimitiveType.DECIMAL -> FieldType.DECIMAL_ARRAY
                  PrimitiveType.LOCAL_DATE -> FieldType.DATE_ARRAY
                  PrimitiveType.TIME -> FieldType.TIME_ARRAY
                  PrimitiveType.DATE_TIME -> FieldType.TIMESTAMP_ARRAY
                  PrimitiveType.INSTANT -> FieldType.TIMESTAMP_WITH_TIMEZONE_ARRAY
                  PrimitiveType.ANY -> FieldType.CHAR_ARRAY
                  PrimitiveType.DOUBLE -> FieldType.DOUBLE_ARRAY
                  PrimitiveType.VOID -> null
                  PrimitiveType.NOTHING -> null
               }
            } else {
               require(memberType is ObjectType) { "Array member type is neither primitive, nor an ObjectType - found ${memberType::class.simpleName}" }
               FieldType.PORTABLE_ARRAY
            }
         }

         type is ObjectType -> FieldType.PORTABLE
         else -> error("Unhandled scenario detecting FieldType - type ${type.qualifiedName}")
      }
   }
}

/**
 * This is a nasty workaround.
 * Hazelcast methods require the correct typed array (ie., String[]).
 * However, when calling typedInstance.toRawObject(), List<T> becomes Any[],
 * which is not castable to String[].
 * Therefore, we need to build the correct array
 */
private fun <T> List<*>.toTypedArray(type: Class<T>):Array<out Any> {
   fun <K> typedGet(index: Int): K {
      return this[index] as K
   }
   val array = when(type) {
      String::class.java -> Array<String>(this.size,  ::typedGet)
      Int::class.java -> Array<Int>(this.size, ::typedGet)
      BigDecimal::class.java -> Array<BigDecimal>(this.size, ::typedGet)
      Boolean::class.java -> Array<Boolean>(this.size, ::typedGet)
      Double::class.java -> Array<Double>(this.size, ::typedGet)
      LocalTime::class.java -> Array<LocalTime>(this.size, ::typedGet)
      LocalDate::class.java -> Array<LocalDate>(this.size, ::typedGet)
      else -> error("Type coercion not implemented for array type ${type.simpleName}")
   }
   return array
}


internal fun getHazelcastFormat(type: com.orbitalhq.schemas.Type) = when {
   type.hasMetadata(HazelcastTaxi.Annotations.CompactObject) -> GenericRecordWriter.Format.Compact
   else -> error("${type.name.longDisplayName} is does not have a Hazelcast serialization. Add a @Compact annotation")
}
