package io.vyne.spring.storeconfigurators

import io.vyne.schemaSpring.StoreConfigurator
import io.vyne.schemaSpring.VyneConsumerRegistrar
import io.vyne.schemaSpring.VynePublisherRegistrar
import mu.KotlinLogging
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.type.AnnotationMetadata

private val logger = KotlinLogging.logger { }

@Deprecated("Use --vyne.schema.publicationMethod and --vyne.schem.consumptionMethod settings to initialise schema publishers and consumers")
object RemoteStoreConfigurator : StoreConfigurator {
   override fun configure(
      importingClassMetadata: AnnotationMetadata,
      registry: BeanDefinitionRegistry,
      environment: ConfigurableEnvironment,
      schemaSourcedProviderRegistrar: (schemaStoreClientBeanName: String) -> Unit) {
      logger.info { "Using an Http based schema store" }
      VynePublisherRegistrar().apply {
         setEnvironment(environment)
         registerHttpPublisher(registry)
      }
      VyneConsumerRegistrar().apply {
         setEnvironment(environment)
         registerHttpConsumer(registry)
      }
      schemaSourcedProviderRegistrar("httpSchemaStoreClient")
   }
}

