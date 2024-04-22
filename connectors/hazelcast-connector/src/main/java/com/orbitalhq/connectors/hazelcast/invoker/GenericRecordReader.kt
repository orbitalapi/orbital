package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.nio.serialization.genericrecord.GenericRecord
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import lang.taxi.types.Arrays
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType

object GenericRecordReader {
   fun toTypedInstance(
      genericRecord: GenericRecord,
      type: ObjectType,
      schema: Schema,
      dataSource: DataSource
   ): TypedInstance {
      val map = readAsMap(genericRecord, type, schema)
      val fromMap = TypedInstance.from(schema.type(type), map, schema, source = dataSource)
      return fromMap
   }
   private fun readAsMap(genericRecord: GenericRecord?, type: ObjectType, schema: Schema):Map<String,Any?>? {
      if (genericRecord == null) {
         return null
      }
      return type.allFields.associate { field ->
         field.name to readFieldValue(field, genericRecord, schema)
      }
   }

   private fun readFieldValue(field: Field, genericRecord: GenericRecord, schema: Schema): Any? {
      val type = field.type
      if (type.inheritsFromPrimitive) {
        return when (field.type.basePrimitive!!) {
            PrimitiveType.BOOLEAN -> genericRecord.getBoolean(field.name)
            PrimitiveType.STRING -> genericRecord.getString(field.name)
            PrimitiveType.INTEGER -> genericRecord.getInt32(field.name)
            PrimitiveType.DECIMAL -> genericRecord.getDecimal(field.name)
            PrimitiveType.LOCAL_DATE -> genericRecord.getDate(field.name)
            PrimitiveType.TIME -> genericRecord.getTime(field.name)
            PrimitiveType.DATE_TIME -> genericRecord.getTimestamp(field.name)
            PrimitiveType.INSTANT -> genericRecord.getTimestampWithTimezone(field.name)
            PrimitiveType.DOUBLE -> genericRecord.getFloat64(field.name)
            else -> error("Deserialization of Hazelcast GenericRecord not supported for Taxi Type ${field.type.basePrimitive!!.name}")
         }
      }
      if (Arrays.isArray(field.type)) {
         val memberType = Arrays.unwrapPossibleArrayType(field.type)
         if (memberType.inheritsFromPrimitive) {
            return when (memberType.basePrimitive!!) {
               PrimitiveType.BOOLEAN -> genericRecord.getArrayOfNullableBoolean(field.name)
               PrimitiveType.STRING -> genericRecord.getArrayOfString(field.name)
               PrimitiveType.INTEGER -> genericRecord.getArrayOfNullableInt32(field.name)
               PrimitiveType.DECIMAL -> genericRecord.getArrayOfDecimal(field.name)
               PrimitiveType.LOCAL_DATE -> genericRecord.getArrayOfDate(field.name)
               PrimitiveType.TIME -> genericRecord.getArrayOfTime(field.name)
               PrimitiveType.DATE_TIME -> genericRecord.getArrayOfTimestamp(field.name)
               PrimitiveType.INSTANT -> genericRecord.getArrayOfTimestampWithTimezone(field.name)
               PrimitiveType.DOUBLE -> genericRecord.getArrayOfFloat64(field.name)
               else -> error("Deserialization of Hazelcast GenericRecord not supported for Taxi Type ${field.type.toQualifiedName().parameterizedName}")
            }
         } else if (memberType is ObjectType) {
            val arrayOfRecords = genericRecord.getArrayOfGenericRecord(field.name) ?: return null
            val values = arrayOfRecords.map { readAsMap(it, memberType, schema) }
            return values
         }

      }
      if (field.type is ObjectType) {
         val fieldGenericRecord = genericRecord.getGenericRecord(field.name)
         return readAsMap(fieldGenericRecord,field.type as ObjectType, schema)
      }
      error("No matching deserialization strategy was found for type ${field.type.toQualifiedName().parameterizedName}")
   }
}
