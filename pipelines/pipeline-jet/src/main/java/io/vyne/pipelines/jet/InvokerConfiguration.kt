package io.vyne.pipelines.jet

import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.lambda.LambdaInvoker
import io.vyne.connectors.aws.s3.S3Invoker
import io.vyne.connectors.aws.sqs.SqsInvoker
import io.vyne.connectors.aws.sqs.SqsStreamManager
import io.vyne.connectors.azure.blob.AzureStreamProvider
import io.vyne.connectors.azure.blob.StoreInvoker
import io.vyne.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.connectors.kafka.KafkaInvoker
import io.vyne.connectors.kafka.KafkaStreamManager
import io.vyne.connectors.kafka.registry.KafkaConnectionRegistry
import io.vyne.schema.api.SchemaProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
/**
 * Registers all operation invokers available. This is a bit duplication of what we do in vyne-query-service.
 * Address this by potentially moving this to a common module which can be consumed by both jet and vyne-query-server.
 */
class InvokerConfiguration {
   @Bean
   fun jdbcInvoker(
      connectionFactory: JdbcConnectionFactory,
      schemaProvider: SchemaProvider
   ): JdbcInvoker {
      return JdbcInvoker(
         connectionFactory, schemaProvider
      )
   }

   @Bean
   fun s3Invoker(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry): S3Invoker {
      return S3Invoker(awsConnectionRegistry, schemaProvider)
   }

   @Bean
   fun sqsStreamManager(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry) =
      SqsStreamManager(awsConnectionRegistry, schemaProvider)


   @Bean
   fun sqsInvoker(schemaProvider: SchemaProvider, sqsStreamManager: SqsStreamManager) =
      SqsInvoker(schemaProvider, sqsStreamManager)

   @Bean
   fun lambdaInvokder(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry): LambdaInvoker {
      return LambdaInvoker(connectionRegistry = awsConnectionRegistry, schemaProvider = schemaProvider)
   }

   @Bean
   fun kafkaStreamManager(
      connectionRegistry: KafkaConnectionRegistry,
      schemaProvider: SchemaProvider
   ) = KafkaStreamManager(connectionRegistry, schemaProvider)

   @Bean
   fun kafkaInvoker(
      streamManager: KafkaStreamManager
   ): KafkaInvoker {
      return KafkaInvoker(
         streamManager
      )
   }

   @Bean
   fun azureStoreInvoker(schemaProvider: SchemaProvider, azureConnectionRegistry: AzureStoreConnectionFileRegistry): StoreInvoker {
      return StoreInvoker(AzureStreamProvider(), azureConnectionRegistry, schemaProvider)
   }
}
