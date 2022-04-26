package io.vyne.schema.spring.config.publisher

import io.vyne.schema.spring.config.SchemaConfigProperties
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata


@Configuration
@Import(
   HttpSchemaPublisherConfig::class,
   RSocketPublisherConfig::class
)
@EnableConfigurationProperties(
   SchemaPublisherConfigProperties::class,
   SchemaConfigProperties::class

)
class SchemaPublisherConfig {

}


class VynePublisherRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private lateinit var environment: ConfigurableEnvironment
   override fun setEnvironment(env: Environment) {
      this.environment = env as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      super.registerBeanDefinitions(importingClassMetadata, registry)
//      SchemaSourceProviderRegistrar.registerSchemaSourceProvider(
//         registry,
//         importingClassMetadata,
//         environment,
//         vyneSchemaPublisherAttributes(importingClassMetadata)
//      )
   }

//   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
//      val consumptionMethodString: String? = environment.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD)
//      consumptionMethodString?.let {
//         when(VyneSchemaInteractionMethod.tryParse(it)) {
//            VyneSchemaInteractionMethod.RSOCKET -> {
//               VyneConsumerRegistrar.registerRSocketProxy(environment, registry)
//               val publisherConfiguration = publisherConfiguration(environment)
//               registry.registerBeanDefinition("rSocketSchemaPublisher",
//                  BeanDefinitionBuilder
//                     .genericBeanDefinition(RSocketSchemaPublisher::class.java)
//                     .addConstructorArgValue(publisherConfiguration)
//                     .beanDefinition
//               )
//               SchemaSourceProviderRegistrar
//                  .registerSchemaSourceProvider(registry,
//                     "",
//                     importingClassMetadata,
//                     environment,
//                     vyneSchemaPublisherAttributes(importingClassMetadata))
//            }
//
//            VyneSchemaInteractionMethod.HTTP -> {
//               registerHttpPublisher(registry)
//               SchemaSourceProviderRegistrar
//                  .registerSchemaSourceProvider(registry,
//                     "",
//                     importingClassMetadata,
//                     environment,
//                     vyneSchemaPublisherAttributes(importingClassMetadata))
//            }
//         }
//      }
//   }

//   fun registerHttpPublisher(registry: BeanDefinitionRegistry) {
//      // Enable relevant feign client.
//      val httpSchemaStoreFeignConfigClass = HttpSchemaSchemaSubmitterFeignConfig::class.java
//      registry.registerBeanDefinition(
//         httpSchemaStoreFeignConfigClass.simpleName,
//         BeanDefinitionBuilder.genericBeanDefinition(httpSchemaStoreFeignConfigClass).beanDefinition
//      )
//
//      // Inject HttpSchemaPublisher
//      val publisherConfiguration = publisherConfiguration(environment)
//      logger.warn { "Registering HttpSchemaStoreClient with publisher configuration: $publisherConfiguration" }
//      registry.registerBeanDefinition("httpSchemaPublisher",
//         BeanDefinitionBuilder.genericBeanDefinition(HttpSchemaPublisher::class.java)
//            .addConstructorArgValue(publisherConfiguration)
//            .beanDefinition
//      )
//   }

   companion object {
      const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"

      private fun vyneSchemaPublisherAttributes(importingClassMetadata: AnnotationMetadata) =
         importingClassMetadata.getAnnotationAttributes("io.vyne.spring.VyneSchemaPublisher") ?: emptyMap()


//
//      fun publisherConfiguration(
//         @Value("\${$VyneRemotePublisherHttpKeepAliveStrategyId:None}") keepAliveStrategyId: KeepAliveStrategyId,
//         @Value("\${spring.application.name:random.uuid}") publisherId: String
//      ): PublisherConfiguration {
////         val keepAliveStrategyId = environment.getProperty(VyneRemotePublisherHttpKeepAliveStrategyId)?.let {
////            KeepAliveStrategyId.tryParse(keepAliveStrategy)
////         } ?: KeepAliveStrategyId.None
////         val publisherId = environment.getProperty("spring.application.name", UUID.randomUUID().toString())
//
//         return when (keepAliveStrategyId) {
//            KeepAliveStrategyId.None -> PublisherConfiguration(publisherId, ManualRemoval)
//            KeepAliveStrategyId.HttpPoll -> {
//
//               val pollUrl = environment.getProperty(VyneRemotePublisherHttpKeepAliveUrl)
//               if (pollUrl == null) {
//                  throw IllegalStateException("$pollUrl can not be empty in @VyneSchemaPublisher when publication method is remote!")
//               }
//               val pollDurationInSeconds =
//                  environment.getProperty(VyneRemotePublisherHttpKeepAlivePingSeconds, Long::class.java, 30L)
//               PublisherConfiguration(
//                  publisherId,
//                  HttpPollKeepAlive(Duration.ofSeconds(pollDurationInSeconds), pollUrl)
//               )
//            }
//            KeepAliveStrategyId.RSocket -> PublisherConfiguration(publisherId, RSocketKeepAlive)
//         }
//      }
   }
}
