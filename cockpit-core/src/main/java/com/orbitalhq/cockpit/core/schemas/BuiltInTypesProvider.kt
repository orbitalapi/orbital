package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.*
import com.orbitalhq.cockpit.core.catalog.DataOwnerAnnotations
import com.orbitalhq.connectors.aws.lambda.LambdaConnectorTaxi
import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi
import com.orbitalhq.connectors.aws.sqs.SqsConnectorTaxi
import com.orbitalhq.connectors.azure.blob.AzureStoreConnectionTaxi
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.kafka.KafkaConnectorTaxi
import com.orbitalhq.formats.csv.CsvAnnotationSpec
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.publisher.SchemaPublisherService
import com.orbitalhq.schemas.taxi.toMessage
import mu.KotlinLogging

object BuiltInTypesProvider {
   private val builtInSources = SourcePackage(
      PackageMetadata.from("com.orbitalhq", "core-types", "1.0.0"),
      listOf(
         VersionedSource(
            "UserTypes",
            "0.1.0",
            UserType.USERNAME_TYPEDEF
         ),
         ErrorType.queryErrorVersionedSource,
         VersionedSource(
            "JdbcConnectors",
            "0.1.0",
            JdbcConnectorTaxi.schema
         ),
         VersionedSource(
            "TaxiQL",
            version = "0.1.0",
            VyneQlGrammar.QUERY_TYPE_TAXI
         ),
         VersionedSource(
            "KafkaConnectors",
            "0.1.0",
            KafkaConnectorTaxi.schema
         ),
         VersionedSource(
            "Catalog",
            "0.1.0",
            DataOwnerAnnotations.schema
         ),
         VersionedSource(
            "AwsS3Connectors",
            "0.1.0",
            S3ConnectorTaxi.schema
         ),
         VersionedSource(
            "AwsSqsConnectors",
            "0.1.0",
            SqsConnectorTaxi.schema
         ),
         VersionedSource(
            "AzureStoreConnectors",
            "0.1.0",
            AzureStoreConnectionTaxi.schema
         ),
         VersionedSource(
            "AwsLambdaConnectors",
            "0.1.0",
            LambdaConnectorTaxi.schema
         ),
         VersionedSource(
            "CsvFormat",
            "0.1.0",
            CsvAnnotationSpec.taxi
         )
      ),
      emptyMap()
   )
   private val builtInTypesSource = builtInSources.sources.joinToString("\n") { it.content }
   val sourcePackage = builtInSources


   // TODO  :Add the others here
   private val builtInNamespaces = listOf("com.orbitalhq", "taxi.stdlib")
   fun isInternalNamespace(namespace: String): Boolean {
      return builtInNamespaces.any { namespace.startsWith(it) }
   }
}


class BuiltInTypesSubmitter(publisherService: SchemaPublisherService) {
   private val logger = KotlinLogging.logger {}

   init {
      logger.info { "Publishing built-in types => ${BuiltInTypesProvider.sourcePackage.sources.map { it.name }}" }
      publisherService.publish(BuiltInTypesProvider.sourcePackage)
         .subscribe { response ->
            if (response.isValid) {
               logger.info { "Built in types published successfully" }
            } else {
               logger.warn { "Publication of built-in types was rejected: \n ${response.errors.toMessage()}" }
            }
         }
   }
}
