package io.vyne.spring.storeconfigurators

import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

//@Deprecated("Use --vyne.schema.publicationMethod and --vyne.schem.consumptionMethod settings to initialise schema publishers and consumers")
//object RemoteStoreConfigurator : StoreConfigurator {
//   override fun configure(
//      importingClassMetadata: AnnotationMetadata,
//      registry: BeanDefinitionRegistry,
//      environment: ConfigurableEnvironment,
//      schemaSourcedProviderRegistrar: (schemaStoreClientBeanName: String) -> Unit) {
//      logger.info { "Using an Http based schema store" }
//      VynePublisherRegistrar().apply {
//         setEnvironment(environment)
//         registerHttpPublisher(registry)
//      }
//      VyneConsumerRegistrar().apply {
//         setEnvironment(environment)
//         registerHttpConsumer(registry)
//      }
//      schemaSourcedProviderRegistrar("httpSchemaStoreClient")
//   }
//}
//
