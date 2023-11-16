package com.orbitalhq.pipelines.jet

import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.lambda.LambdaInvoker
import com.orbitalhq.connectors.aws.s3.S3Invoker
import com.orbitalhq.connectors.aws.sqs.SqsInvoker
import com.orbitalhq.connectors.aws.sqs.SqsStreamManager
import com.orbitalhq.connectors.azure.blob.AzureStreamProvider
import com.orbitalhq.connectors.azure.blob.StoreInvoker
import com.orbitalhq.connectors.azure.blob.registry.AzureStoreConnectionFileRegistry
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.kafka.KafkaInvoker
import com.orbitalhq.connectors.kafka.KafkaStreamManager
import com.orbitalhq.connectors.kafka.KafkaStreamPublisher
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.schema.api.SchemaProvider
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner
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
      schemaProvider: SchemaProvider,
      formatRegistry: FormatRegistry
   ) = KafkaStreamManager(connectionRegistry, schemaProvider, formatRegistry = formatRegistry)

   @Bean
   fun kafkaStreamPublisher(
      connectionRegistry: KafkaConnectionRegistry,
      formatRegistry: FormatRegistry
   ): KafkaStreamPublisher {
      return KafkaStreamPublisher(connectionRegistry, formatRegistry = formatRegistry)
   }

   @Bean
   fun kafkaInvoker(
      streamManager: KafkaStreamManager,
      streamPublisher: KafkaStreamPublisher
   ): KafkaInvoker {
      return KafkaInvoker(
         streamManager,
         streamPublisher
      )
   }

   @Bean
   fun azureStoreInvoker(
      schemaProvider: SchemaProvider,
      azureConnectionRegistry: AzureStoreConnectionFileRegistry
   ): StoreInvoker {
      return StoreInvoker(AzureStreamProvider(), azureConnectionRegistry, schemaProvider)
   }
}
