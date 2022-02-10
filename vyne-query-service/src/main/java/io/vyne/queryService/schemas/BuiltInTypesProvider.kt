package io.vyne.queryService.schemas

import io.vyne.VersionedSource
import io.vyne.connectors.aws.s3.S3ConnectorTaxi
import io.vyne.connectors.aws.sqs.SqsConnectorTaxi
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.kafka.KafkaConnectorTaxi
import io.vyne.query.VyneQlGrammar
import io.vyne.queryService.catalog.DataOwnerAnnotations
import io.vyne.queryService.security.VyneUser
import lang.taxi.Compiler

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
      )
   )
   private val builtInTypesSource = builtInSources.joinToString("\n") { it.content }
   private val taxiDocument = Compiler(builtInTypesSource).compile()
   val versionedSources = builtInSources

}
