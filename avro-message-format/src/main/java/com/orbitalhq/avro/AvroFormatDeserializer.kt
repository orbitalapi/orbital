package com.orbitalhq.avro

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import mu.KotlinLogging
import org.apache.avro.AvroRuntimeException
import org.apache.avro.generic.GenericArray
import org.apache.avro.generic.GenericContainer
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.util.Utf8
import java.lang.Exception
import java.nio.ByteBuffer

class AvroFormatDeserializer(
   private val schemaCache: AvroSchemaCache,
   private val decoderFactory: DecoderFactory = DecoderFactory.get()
) : ModelFormatDeserializer {
   companion object {
      /**
       * Confluent embed a "magic byte" at the start of their avro encoded
       * messages, to indicate that the message is the confluent variation of Avro.
       */
      const val CONFLUENT_MAGIC_BYTE: Byte = 0;
      private val logger = KotlinLogging.logger {}
   }


   override fun canParse(value: Any, metadata: Metadata, type: Type): Boolean = value is ByteArray || value is String

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any {
      return decodeAvroMessage(schema, type, value, metadata, source)
   }

   private fun decodeAvroMessage(
      schema: Schema,
      type: Type,
      value: Any,
      metadata: Metadata,
      source: DataSource,
      allowDetectionOfConfluentEncoding: Boolean = true
   ): Any {
      var detectedConfluentEncoding = false
      val avroSchema = schemaCache.get(schema, type)
      val decoder = when (value) {
         is ByteArray -> {
            // When reading a byte array, we need to determine how it was encoded ...
            // either as JSON or binary
            if (isJsonString(value)) {
               decoderFactory.jsonDecoder(avroSchema, String(value))
            } else if (allowDetectionOfConfluentEncoding && isConfluentAvroFormat(value)) {
               // Strip off the Confluent-specific prelude.
               val avroWithoutConfluentPrelude = value.copyOfRange(5, value.size)
               detectedConfluentEncoding = true
               decoderFactory.binaryDecoder(avroWithoutConfluentPrelude, null)
            } else {
               decoderFactory.binaryDecoder(value, null)
            }
         }

         is String -> {
            if (value.trim().startsWith("[") || value.trim().startsWith("{")) {
               DecoderFactory.get().jsonDecoder(avroSchema, value)
            } else {
               return parse(value.toByteArray(), type, metadata, schema, source)
            }
         }

         else -> error("Decoding Avro from input type ${value::class.simpleName} is not supported")
      }
      val reader = if (type.isCollection) {
         GenericDatumReader<GenericArray<GenericRecord>>(avroSchema)

      } else {
         GenericDatumReader<GenericRecord>(avroSchema)
      }
      val deserializedRecord = try {
         reader.read(null, decoder)
      } catch (e: AvroRuntimeException) {
         if (e.message.orEmpty()
               .startsWith("Malformed data. Length is negative") && detectedConfluentEncoding && allowDetectionOfConfluentEncoding
         ) {
            // Last ditch attempt.
            // Try and read the original, non-stripped message
            logger.warn { "An Avro message was detected as using Confluent encoding, which then subsequently threw an exception when decoding. Attempting to fall back to vanilla format" }
            return decodeAvroMessage(schema, type, value, metadata, source, allowDetectionOfConfluentEncoding = false)
         } else {
            throw e
         }
      } catch (e: Exception) {
         throw e
      }

      val rawValue = genericContainerToRawValue(deserializedRecord)
      return TypedInstance.from(type, rawValue, schema, source = source)
   }

   /**
    * Attempts to detect if a message is encoded using Confluents 'magic byte' at the start.
    * This is a best-effort attempt, and doesn't always work.
    * We need to add support for a dedicated annotation -- see ORB-970
    */
   private fun isConfluentAvroFormat(value: ByteArray): Boolean {
      if (value.size < 5) return false
      return value[0] == CONFLUENT_MAGIC_BYTE
   }

   private fun isJsonString(data: ByteArray): Boolean {
      // Check if the first non-whitespace character is '{' or '['
      val firstChar = data.firstOrNull { !it.toChar().isWhitespace() }?.toChar()
      return firstChar == '{' || firstChar == '['
   }

   private fun genericContainerToRawValue(rawValue: GenericContainer): Any {
      return when (rawValue) {
         is GenericData.Array<*> -> generateArrayToList(rawValue)
         is GenericData.Record -> genericRecordToMap(rawValue)
         else -> error("Unhandled root type in Avro deserialization: ${rawValue::class.simpleName}")
      }
   }

   private fun generateArrayToList(record: GenericData.Array<*>): List<Any> {
      return record.map {
         val member = it as GenericContainer
         genericContainerToRawValue(member)
      }
   }

   private fun genericRecordToMap(record: GenericData.Record): Map<String, Any?> {
      val map = mutableMapOf<String, Any?>()
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

            is Map<*, *> -> {
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
