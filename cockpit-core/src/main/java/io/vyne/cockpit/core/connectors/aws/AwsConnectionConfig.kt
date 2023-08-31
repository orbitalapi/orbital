package io.vyne.cockpit.core.connectors.aws

import io.vyne.connectors.VyneConnectionsConfig
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.core.registry.SourceLoaderAwsConnectionRegistry
import io.vyne.connectors.aws.dynamodb.DynamoDbInvoker
import io.vyne.connectors.aws.lambda.LambdaInvoker
import io.vyne.connectors.aws.s3.S3Invoker
import io.vyne.connectors.aws.sqs.SqsInvoker
import io.vyne.connectors.aws.sqs.SqsStreamManager
import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import io.vyne.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class AwsConnectionConfig {
   @Bean
   fun awsConnectionRegistry(config: SourceLoaderConnectorsRegistry): AwsConnectionRegistry {
       return SourceLoaderAwsConnectionRegistry(config)
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
   fun lambdaInvoker(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry): LambdaInvoker {
      return LambdaInvoker(connectionRegistry = awsConnectionRegistry, schemaProvider = schemaProvider)
   }

    @Bean
    fun dynamoDbInvoker(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry): DynamoDbInvoker {
        return DynamoDbInvoker(connectionRegistry = awsConnectionRegistry, schemaProvider = schemaProvider)
    }
}
