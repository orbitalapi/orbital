package io.vyne.queryService.schemas

import io.vyne.VersionedSource
import io.vyne.connectors.aws.lambda.LambdaConnectorTaxi
import io.vyne.connectors.aws.s3.S3ConnectorTaxi
import io.vyne.connectors.aws.sqs.SqsConnectorTaxi
import io.vyne.connectors.azure.blob.AzureStoreConnectionTaxi
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.kafka.KafkaConnectorTaxi
import io.vyne.models.csv.CsvAnnotationSpec
import io.vyne.query.VyneQlGrammar
import io.vyne.queryService.catalog.DataOwnerAnnotations
import io.vyne.queryService.security.VyneUser
import io.vyne.schema.publisher.SchemaPublisherService
import io.vyne.schemas.taxi.toMessage
import lang.taxi.Compiler
import mu.KotlinLogging
import org.springframework.stereotype.Component

object VyneTypes {
   const val NAMESPACE = "io.vyne"
}

object BuiltInTypesProvider {
   private val builtInSources = listOf(
      VersionedSource(
         "UserTypes",
         "0.1.0",
         VyneUser.USERNAME_TYPEDEF
      ),
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
   private val builtInTypesSource = builtInSources.joinToString("\n") { it.content }
   private val taxiDocument = Compiler(builtInTypesSource).compile()
   val versionedSources = builtInSources

}


@Component
class BuiltInTypesSubmitter(publisherService: SchemaPublisherService) {
   private val logger = KotlinLogging.logger {}

   init {
      logger.info { "Publishing built-in types" }
      publisherService.publish(BuiltInTypesProvider.versionedSources)
         .subscribe { response ->
            if (response.isValid) {
               logger.info { "Built in types published successfully" }
            } else {
               logger.warn { "Publication of built-in types was rejected: \n ${response.errors.toMessage()}" }
            }

         }
   }
}
