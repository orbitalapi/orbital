package io.vyne.queryService.schemas.importing.kafka

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.connectors.kafka.KafkaConnectorTaxi
import io.vyne.queryService.schemas.editor.EditedSchema
import io.vyne.queryService.schemas.editor.generator.VyneSchemaToTaxiGenerator
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
import io.vyne.queryService.schemas.importing.SchemaConverter
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringDeserializer
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.NamingUtils.replaceIllegalCharacters
import lang.taxi.generators.NamingUtils.toCapitalizedWords
import lang.taxi.types.StreamType
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

data class KafkaTopicConverterOptions(
   val connectionName: String,
   val topicName: String,
   val offset: KafkaConnectorTaxi.Annotations.KafkaOperation.Offset,
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val messageType: QualifiedName,
   val targetNamespace: String = messageType.namespace,
   val serviceName: String? = null,
   val operationName: String? = null
)

@Component
class KafkaTopicImporter(val schemaProvider: SchemaProvider) : SchemaConverter<KafkaTopicConverterOptions> {
   companion object {
      const val SUPPORTED_FORMAT = "kafkaTopic"
   }

   override val supportedFormats: List<String> = listOf(SUPPORTED_FORMAT)

   override val conversionParamsType: KClass<KafkaTopicConverterOptions> = KafkaTopicConverterOptions::class

   override fun convert(
      request: SchemaConversionRequest,
      options: KafkaTopicConverterOptions
   ): Mono<GeneratedTaxiCode> {
      val generator = StreamingMessageServiceGenerator()
      val schema = schemaProvider.schema()
      val returnType = schema.type(options.messageType)
      val serviceName = options.serviceName?.fqn() ?: KafkaTopicMapping.defaultServiceName(
         options.targetNamespace,
         options.connectionName
      )
      val operationName = options.operationName?.let { operationName ->
         OperationNames.name(
            serviceName.fullyQualifiedName,
            operationName
         ).fqn()
      } ?: KafkaTopicMapping.defaultOperationName(serviceName, options.topicName)
      val mappingRequest = KafkaTopicMapping(
         options.connectionName,
         options.topicName,
         options.offset,
         operationName
      )
      return Mono.just(
         generator.createServiceTaxi(
            mappingRequest,
            returnType,
            schema
         )
      )
   }
}




data class KafkaTopicMapping(
   val connectionName: String,
   val topicName: String,
   val offset: KafkaConnectorTaxi.Annotations.KafkaOperation.Offset,
   val operationName: QualifiedName
) {
   companion object {
      fun defaultServiceName(namespace: String, connectionName: String): QualifiedName {
         return "$namespace.${connectionName.replaceIllegalCharacters().toCapitalizedWords()}Service".fqn()
      }

      fun defaultOperationName(serviceName: QualifiedName, topicName: String): QualifiedName {
         return OperationNames.name(
            serviceName.fullyQualifiedName,
            "consumeFrom${topicName.replaceIllegalCharacters().toCapitalizedWords()}"
         ).fqn()
      }
   }
}


class StreamingMessageServiceGenerator(
   private val taxiGenerator: VyneSchemaToTaxiGenerator = VyneSchemaToTaxiGenerator()
) {

   fun createService(mapping: KafkaTopicMapping, returnType: Type): Service {
      val operation = Operation(
         mapping.operationName,
         emptyList(),
         returnType,
         metadata = listOf(
            KafkaConnectorTaxi.Annotations.KafkaOperation(
               mapping.topicName,
               mapping.offset
            ).asMetadata()
         ),
         sources = emptyList()
      )

      val service = Service(
         name = OperationNames.serviceName(mapping.operationName).fqn(),
         operations = listOf(operation),
         queryOperations = emptyList(),
         metadata = listOf(
            KafkaConnectorTaxi.Annotations.KafkaService(
               mapping.connectionName
            ).asMetadata()
         ),
         sourceCode = emptyList()
      )
      return service
   }

   fun createServiceTaxi(mapping: KafkaTopicMapping, returnType: Type, currentSchema: Schema): GeneratedTaxiCode {

      val streamReturnType = currentSchema.type(StreamType.of(returnType.taxiType))
      val service = createService(mapping, streamReturnType)
      return taxiGenerator.generate(
         EditedSchema(
            types = emptySet(),
            services = setOf(service)
         ),
         currentSchema.asTaxiSchema()
      )
   }
}
