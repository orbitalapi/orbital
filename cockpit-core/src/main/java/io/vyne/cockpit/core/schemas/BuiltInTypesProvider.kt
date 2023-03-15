package io.vyne.cockpit.core.schemas

import io.vyne.ErrorType
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.UserType
import io.vyne.VersionedSource
import io.vyne.cockpit.core.catalog.DataOwnerAnnotations
import io.vyne.connectors.aws.lambda.LambdaConnectorTaxi
import io.vyne.connectors.aws.s3.S3ConnectorTaxi
import io.vyne.connectors.aws.sqs.SqsConnectorTaxi
import io.vyne.connectors.azure.blob.AzureStoreConnectionTaxi
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.kafka.KafkaConnectorTaxi
import io.vyne.models.csv.CsvAnnotationSpec
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.publisher.SchemaPublisherService
import io.vyne.schemas.taxi.toMessage
import mu.KotlinLogging

object BuiltInTypesProvider {
   private val builtInSources = SourcePackage(
      PackageMetadata.from("io.vyne", "core-types", "1.0.0"),
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
      )
   )
   private val builtInTypesSource = builtInSources.sources.joinToString("\n") { it.content }
   val sourcePackage = builtInSources
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
