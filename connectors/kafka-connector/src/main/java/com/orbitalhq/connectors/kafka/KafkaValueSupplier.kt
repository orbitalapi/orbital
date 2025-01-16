package com.orbitalhq.connectors.kafka

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.ValueSupplier
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.EnumMember
import reactor.kafka.receiver.ReceiverRecord

/**
 * Supports exposing keys and headers to the TypedObject construction process
 */
class KafkaValueSupplier(private val record: ReceiverRecord<Any, ByteArray>) : ValueSupplier {
   private val supportedMetadataNames = setOf(
      KafkaConnectorTaxi.Annotations.KafkaHeader.NAME,
      KafkaConnectorTaxi.Annotations.KafkaMessageKey.NAME,
      KafkaConnectorTaxi.Annotations.KafkaMessageMetadata.NAME,
   )

   override fun canSupply(field: Field, fieldType: Type): Boolean {
      return hasSupportedMetadata(field.metadata) || hasSupportedMetadata(fieldType.metadata)
   }

   private fun hasSupportedMetadata(metadata: List<Metadata>): Boolean {
      return metadata.any { supportedMetadataNames.contains(it.name.fullyQualifiedName) }
   }

   private fun supplyHeaderValue(header: Metadata, fieldType: Type, schema: Schema, source: DataSource): TypedInstance {

      val headerName = header.params["value"] as String? ?:
         // This is an error, because it shouldn't happen - the compiler should detect
         // an annotation that was declared without the name param
         error("Error reading type metadata - expected to receive a default parameter but was not present")
      val recordHeaders = record.headers().headers(headerName).toList()

      return when {
         recordHeaders.isEmpty() -> TypedNull.create(fieldType, source)
         else -> TypedInstance.from(fieldType, String(recordHeaders.single().value()), schema, source = source)
      }
   }

   private fun supplyMessageKey(fieldType: Type, source: DataSource, schema: Schema): TypedInstance {
      return TypedInstance.from(fieldType, record.key(), schema, source = source)
   }

   override fun supplyValue(
      field: Field,
      fieldType: Type,
      schema: Schema,
      source: DataSource,
      typedObjectFactory: TypedObjectFactory
   ): TypedInstance {
      val metadata = field.metadata + fieldType.metadata
      val header = metadata.forName(KafkaConnectorTaxi.Annotations.KafkaHeader.NAME)
      if (header != null) {
         return supplyHeaderValue(header, fieldType, schema, source)
      }
      val messageKey =
         metadata.forName(KafkaConnectorTaxi.Annotations.KafkaMessageKey.NAME)
      if (messageKey != null) {
         return supplyMessageKey(fieldType, source, schema)
      }
      val messageMetadata = metadata.forName(KafkaConnectorTaxi.Annotations.KafkaMessageMetadata.NAME)
      if (messageMetadata != null) {
         return supplyMessageMetadata(messageMetadata, fieldType, schema, source)
      }
      error("Expected to supply either a header or message key, but neither were true")
   }

   private fun supplyMessageMetadata(
      messageMetadata: Metadata,
      fieldType: Type,
      schema: Schema,
      source: DataSource
   ): TypedInstance {
      val metadataType = (messageMetadata.params["value"] as String)
      val metadataValue: Any = when (metadataType) {
         "Partition" -> record.partition()
         "Offset" -> record.offset()
         "Timestamp" -> record.timestamp()
         "Topic" -> record.topic()
         "TimestampType" -> record.timestampType().name
         else -> error("Unknown message metadata type: $metadataType")
      }
      return TypedInstance.from(fieldType, metadataValue, schema, source = source)
   }


}
private fun List<Metadata>.forName(name: String):Metadata? {
   return this.firstOrNull { it.name.fullyQualifiedName == name }
}
