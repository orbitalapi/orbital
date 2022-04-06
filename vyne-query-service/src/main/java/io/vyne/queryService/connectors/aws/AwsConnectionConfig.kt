package io.vyne.queryService.connectors.aws

import io.vyne.connectors.aws.core.registry.AwsConfigFileConnectionRegistry
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.aws.lambda.LambdaInvoker
import io.vyne.connectors.aws.s3.S3Invoker
import io.vyne.connectors.aws.sqs.SqsInvoker
import io.vyne.connectors.aws.sqs.SqsStreamManager
import io.vyne.queryService.connectors.jdbc.VyneConnectionsConfig
import io.vyne.schema.api.SchemaProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VyneConnectionsConfig::class)
class AwsConnectionConfig {
   @Bean
   fun awsConnectionRegistry(config: VyneConnectionsConfig): AwsConfigFileConnectionRegistry {
      return AwsConfigFileConnectionRegistry(config.configFile)
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
}
