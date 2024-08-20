package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.AuthClaimType
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.UserType
import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneTypes
import com.orbitalhq.annotations.http.HttpIgnoreErrorsAnnotationSchema
import com.orbitalhq.annotations.http.HttpRetryAnnotationSchema
import com.orbitalhq.cockpit.core.catalog.DataOwnerAnnotations
import com.orbitalhq.connectors.aws.dynamodb.DynamoConnectorTaxi
import com.orbitalhq.connectors.aws.lambda.LambdaConnectorTaxi
import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi
import com.orbitalhq.connectors.aws.sqs.SqsConnectorTaxi
import com.orbitalhq.connectors.azure.blob.AzureStoreConnectionTaxi
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.kafka.KafkaConnectorTaxi
import com.orbitalhq.connectors.nosql.mongodb.MongoConnector
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.formats.csv.CsvAnnotationSpec
import com.orbitalhq.formats.xml.XmlAnnotationSpec
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.publisher.SchemaPublisherService
import com.orbitalhq.schemas.taxi.toMessage
import lang.taxi.annotations.HttpService
import lang.taxi.generators.avro.AvroAnnotationSchema
import mu.KotlinLogging

object BuiltInTypesProvider {
   private val builtInSources = SourcePackage(
      PackageMetadata.from(VyneTypes.NAMESPACE, "core-types", "1.0.0"),
      listOf(
         VersionedSource.unversioned("taxi.http", HttpService.asTaxi()),
         VersionedSource(
            "UserTypes",
            "0.1.0",
            UserType.UsernameTypeDefinition
         ),
         VersionedSource(
            "JwtTypes",
            "0.1.0",
            AuthClaimType.AuthClaimsTypeDefinition
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
            "AwsDynamoConnectors",
            "0.1.0",
            DynamoConnectorTaxi.schema
         ),
         VersionedSource(
            "CsvFormat",
            "0.1.0",
            CsvAnnotationSpec.taxi
         ),
         VersionedSource(
            "XmlFormat",
            "0.1.0",
            XmlAnnotationSpec.taxi
         ),
         VersionedSource(
            "HttpRetryAnnotationSchema",
            "0.1.0",
            HttpRetryAnnotationSchema.schema
         ),
         VersionedSource(
            "HazelcastConnectors",
            "0.1.0",
            HazelcastTaxi.schema
         ),
         VersionedSource(
            "MongoDbConnector",
            "0.1.0",
            MongoConnector.schema
         ),
         VersionedSource(
            "AvroFormat",
            "0.1.0",
            AvroAnnotationSchema.taxi
         ),
         VersionedSource(
            "HttpIgnoreErrorsAnnotationSchema",
            "0.1.0",
            HttpIgnoreErrorsAnnotationSchema.schema
         ),
      ),
      emptyMap()
   )
   val source = builtInSources.sources.joinToString("\n") { it.content }
   val sourcePackage = builtInSources


   // TODO  :Add the others here
   private val builtInNamespaces = listOf(VyneTypes.NAMESPACE, "taxi.stdlib")
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
