package com.orbitalhq.cockpit.core.connectors.aws

import com.orbitalhq.connectors.VyneConnectionsConfig
import com.orbitalhq.connectors.aws.core.registry.AwsConnectionRegistry
import com.orbitalhq.connectors.aws.core.registry.SourceLoaderAwsConnectionRegistry
import com.orbitalhq.connectors.aws.dynamodb.DynamoDbInvoker
import com.orbitalhq.connectors.aws.lambda.LambdaInvoker
import com.orbitalhq.connectors.aws.s3.S3Invoker
import com.orbitalhq.connectors.aws.sqs.SqsConnectionBuilder
import com.orbitalhq.connectors.aws.sqs.SqsInvoker
import com.orbitalhq.connectors.aws.sqs.SqsStreamManager
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.schema.api.SchemaProvider
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
   fun sqsConnectionBuilder(
      awsConnectionRegistry: AwsConnectionRegistry,
      formatRegistry: FormatRegistry
   ) = SqsConnectionBuilder(
      awsConnectionRegistry, formatRegistry
   )

   @Bean
   fun sqsStreamManager(schemaProvider: SchemaProvider, connectionBuilder: SqsConnectionBuilder) =
      SqsStreamManager(connectionBuilder, schemaProvider)


   @Bean
   fun sqsInvoker(schemaProvider: SchemaProvider, sqsStreamManager: SqsStreamManager, connectionBuilder: SqsConnectionBuilder) =
      SqsInvoker(schemaProvider, sqsStreamManager, connectionBuilder)

   @Bean
   fun lambdaInvoker(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry): LambdaInvoker {
      return LambdaInvoker(connectionRegistry = awsConnectionRegistry, schemaProvider = schemaProvider)
   }

    @Bean
    fun dynamoDbInvoker(schemaProvider: SchemaProvider, awsConnectionRegistry: AwsConnectionRegistry): DynamoDbInvoker {
        return DynamoDbInvoker(connectionRegistry = awsConnectionRegistry, schemaProvider = schemaProvider)
    }
}
