package io.vyne.schemaSpring

import io.vyne.httpSchemaPublisher.HttpSchemaPublisher
import io.vyne.rSocketSchemaPublisher.RSocketPublisherKeepAliveStrategyMonitor
import io.vyne.rSocketSchemaPublisher.RSocketSchemaPublisher
import io.vyne.schemaApi.VyneSchemaInteractionMethod
import io.vyne.schemaPublisherApi.HttpPollKeepAlive
import io.vyne.schemaPublisherApi.KeepAliveStrategyId
import io.vyne.schemaPublisherApi.ManualRemoval
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.RSocketKeepAlive
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata
import java.time.Duration
import java.util.UUID

const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"
const val VyneRemoteSchemaStoreTllCheckInSeconds = "vyne.schema.management.ttlCheckInSeconds"
const val VyneRemoteSchemaStoreHttpRequestTimeoutInSeconds = "vyne.schema.management.httpRequestTimeoutInSeconds"
const val VyneRemotePublisherHttpKeepAliveStrategyId = "vyne.schema.management.KeepAliveStrategyId"
const val VyneRemotePublisherHttpKeepAliveUrl = "vyne.schema.management.keepAliveUrl"
const val VyneRemotePublisherHttpKeepAlivePingSeconds = "vyne.schema.management.keepAlivePingDurationInSeconds"

class VynePublisherRegistrar:  ImportBeanDefinitionRegistrar, EnvironmentAware {
   private lateinit var environment: ConfigurableEnvironment
   override fun setEnvironment(env: Environment) {
      this.environment = env as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val consumptionMethodString: String? = environment.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD)
      consumptionMethodString?.let {
         when(VyneSchemaInteractionMethod.tryParse(it)) {
            VyneSchemaInteractionMethod.RSOCKET -> {
               VyneConsumerRegistrar.registerRSocketProxy(environment, registry)
               val publisherConfiguration = publisherConfiguration(environment)
               registry.registerBeanDefinition("rSocketSchemaPublisher",
                  BeanDefinitionBuilder
                     .genericBeanDefinition(RSocketSchemaPublisher::class.java)
                     .addConstructorArgValue(publisherConfiguration)
                     .beanDefinition
               )
               SchemaSourceProviderRegistrar
                  .registerSchemaSourceProvider(registry,
                     "",
                     importingClassMetadata,
                     environment,
                     vyneSchemaPublisherAttributes(importingClassMetadata))
            }

            VyneSchemaInteractionMethod.HTTP -> {
               registerHttpPublisher(registry)
               SchemaSourceProviderRegistrar
                  .registerSchemaSourceProvider(registry,
                     "",
                     importingClassMetadata,
                     environment,
                     vyneSchemaPublisherAttributes(importingClassMetadata))
            }
         }
      }
   }

   fun registerHttpPublisher(registry: BeanDefinitionRegistry) {
      // Enable relevant feign client.
      val httpSchemaStoreFeignConfigClass = HttpSchemaSchemaSubmitterFeignConfig::class.java
      registry.registerBeanDefinition(
         httpSchemaStoreFeignConfigClass.simpleName,
         BeanDefinitionBuilder.genericBeanDefinition(httpSchemaStoreFeignConfigClass).beanDefinition
      )

      // Inject HttpSchemaPublisher
      val publisherConfiguration = publisherConfiguration(environment)
      logger.warn { "Registering HttpSchemaStoreClient with publisher configuration: $publisherConfiguration" }
      registry.registerBeanDefinition("httpSchemaPublisher",
         BeanDefinitionBuilder.genericBeanDefinition(HttpSchemaPublisher::class.java)
            .addConstructorArgValue(publisherConfiguration)
            .beanDefinition
      )
   }

   companion object {
      const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"

      private fun vyneSchemaPublisherAttributes(importingClassMetadata: AnnotationMetadata) =
         importingClassMetadata.getAnnotationAttributes("io.vyne.spring.VyneSchemaPublisher") ?: emptyMap()

       fun publisherConfiguration(environment: ConfigurableEnvironment): PublisherConfiguration {
         val keepAliveStrategyId = environment.getProperty(VyneRemotePublisherHttpKeepAliveStrategyId)?.let {
            KeepAliveStrategyId.tryParse(it)
         } ?: KeepAliveStrategyId.None
         val publisherId = environment.getProperty("spring.application.name", UUID.randomUUID().toString())

         return when (keepAliveStrategyId) {
            KeepAliveStrategyId.None -> PublisherConfiguration(publisherId, ManualRemoval)
            KeepAliveStrategyId.HttpPoll -> {

               val pollUrl = environment.getProperty(VyneRemotePublisherHttpKeepAliveUrl)
               if (pollUrl == null) {
                  throw IllegalStateException("$pollUrl can not be empty in @VyneSchemaPublisher when publication method is remote!")
               }
               val pollDurationInSeconds = environment.getProperty(VyneRemotePublisherHttpKeepAlivePingSeconds, Long::class.java, 30L)
               PublisherConfiguration(publisherId, HttpPollKeepAlive(Duration.ofSeconds(pollDurationInSeconds), pollUrl))
            }
            KeepAliveStrategyId.RSocket -> PublisherConfiguration(publisherId, RSocketKeepAlive)
         }
      }
   }
}
