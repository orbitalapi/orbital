package com.orbitalhq.avro

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import org.apache.avro.generic.GenericArray
import org.apache.avro.generic.GenericContainer
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.util.Utf8

class AvroFormatDeserializer(private val schemaCache: AvroSchemaCache = AvroFormatSpec.newSchemaCache()) :
   ModelFormatDeserializer {

   override fun canParse(value: Any, metadata: Metadata): Boolean = value is ByteArray

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any {
      val avroSchema = schemaCache.get(type to schema)
      require(value is ByteArray) { "AvroFormatDeserializer requires ByteArray input, received ${value::class.simpleName}" }
      val decoder = DecoderFactory.get().binaryDecoder(value, null)
      val reader =if (type.isCollection) {
         GenericDatumReader<GenericArray<GenericRecord>>(avroSchema)

      } else {
         GenericDatumReader<GenericRecord>(avroSchema)
      }
      val deserializedRecord = reader.read(null, decoder)
      val rawValue = genericContainerToRawValue(deserializedRecord)
      return TypedInstance.from(type, rawValue, schema, source = source)
   }

   private fun genericContainerToRawValue(rawValue: GenericContainer):Any {
      return when(rawValue) {
         is GenericData.Array<*> -> generateArrayToList(rawValue)
         is GenericData.Record -> genericRecordToMap(rawValue)
         else -> error("Unhandled root type in Avro deserialization: ${rawValue::class.simpleName}")
      }
   }

   private fun generateArrayToList(record: GenericData.Array<*>):List<Any> {
      return record.map {
         val member = it as GenericContainer
         genericContainerToRawValue(member)
      }
   }

   private fun genericRecordToMap(record: GenericData.Record): Map<String, Any> {
      val map = mutableMapOf<String, Any>()
      val schema = record.schema
      for (field in schema.fields) {
         val fieldValue = record.get(field.name())
         map[field.name()] = when (fieldValue) {
            is GenericData.Record -> genericRecordToMap(fieldValue)
            is GenericData.EnumSymbol -> fieldValue.toString()
            is List<*> -> fieldValue.map { item ->
               when (item) {
                  is GenericData.Record -> genericRecordToMap(item)
                  is Utf8 -> item.toString()
                  else -> item
               }
            }
            is Map<*,*> -> {
               // Have to unwrap avro-special values like Utf8,
               // which are used both in keys and values.
               fieldValue.map { (key, value) ->
                  val mappedValue = when (value) {
                     is Utf8 -> value.toString()
                     else -> value
                  }
                  key.toString() to mappedValue
               }.toMap()
            }
            is Utf8 -> fieldValue.toString()
            else -> fieldValue
         }
      }
      return map
   }
}
