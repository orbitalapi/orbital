package com.orbitalhq.avro

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedEnumValue
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import lang.taxi.types.isMapType
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.io.EncoderFactory
import java.io.ByteArrayOutputStream

class AvroFormatSerializer(private val schemaCache: AvroSchemaCache) :
   ModelFormatSerializer {
   override fun write(
      result: TypedInstance,
      metadata: Metadata,
      schema: Schema,
      index: Int
   ): Any? {
      val type = result.type
      val avroSchema = schemaCache.get(schema, type)
      val genericRecord = buildAvroValue(result, avroSchema)
      val writer = GenericDatumWriter<Any>(avroSchema)
      val outputStream = ByteArrayOutputStream()
      val encoder = EncoderFactory.get().binaryEncoder(outputStream, null)
      writer.write(genericRecord, encoder)
      encoder.flush()
      outputStream.close()

      return outputStream.toByteArray()
   }

   override fun write(rawValue: Any?, metadata: Metadata, index: Int): Any? {
      error("Writing raw values is not supported by Avro")
   }

   private fun buildAvroValue(instance: TypedInstance, avroSchema: org.apache.avro.Schema): Any? {
      return when {
         instance is TypedCollection -> buildAvroArray(instance, avroSchema)
         instance is TypedObject && instance.type.taxiType.isMapType() -> buildAvroMap(instance, avroSchema)
         instance is TypedObject -> buildAvroRecord(instance, avroSchema)
         instance is TypedValue -> instance.value
         instance is TypedNull -> null
         instance is TypedEnumValue -> GenericData.EnumSymbol(avroSchema, instance.value)
         else -> TODO("Support for class ${instance.javaClass.name}")
      }
   }

   private fun buildAvroMap(instance: TypedObject, avroSchema: org.apache.avro.Schema): Map<String, Any> {
      return instance.toRawObject() as Map<String, Any>
   }

   private fun buildAvroArray(instance: TypedCollection, arraySchema: org.apache.avro.Schema): GenericData.Array<*> {
      val values = instance.value.map { member -> buildAvroValue(member, arraySchema.elementType) }
      return GenericData.Array(arraySchema, values)
   }

   private fun buildAvroRecord(instance: TypedObject, avroSchema: org.apache.avro.Schema): GenericData.Record {
      return GenericData.Record(avroSchema).apply {
         instance.entries.forEach { (fieldName, fieldTypedInstance) ->
            val fieldSchema = avroSchema.getField(fieldName).schema()
            val fieldValue = buildAvroValue(fieldTypedInstance, fieldSchema)
            put(fieldName, fieldValue)
         }
      }
   }

   override fun writeAsBytes(result: TypedInstance, metadata: Metadata, schema: Schema, index: Int): ByteArray {
      return write(result, metadata, schema, index) as ByteArray
   }
}
