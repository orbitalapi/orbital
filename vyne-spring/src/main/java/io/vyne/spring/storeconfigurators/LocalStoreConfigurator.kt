//package io.vyne.spring.storeconfigurators
//
//import io.vyne.schema.publisher.http.HttpPollKeepAliveStrategyMonitor
//import io.vyne.schema.publisher.http.HttpPollKeepAliveStrategyPollUrlResolver
//import io.vyne.schema.publisher.NoneKeepAliveStrategyMonitor
//import io.vyne.schema.spring.StoreConfigurator
//import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
//import io.vyne.schemaStore.TaxiSchemaStoreService
//import io.vyne.spring.VyneRemoteSchemaStoreHttpRequestTimeoutInSeconds
//import io.vyne.spring.VyneRemoteSchemaStoreTllCheckInSeconds
//import io.vyne.spring.registerBeanDefinitionOfType
//import mu.KotlinLogging
//import org.springframework.beans.factory.support.AbstractBeanDefinition
//import org.springframework.beans.factory.support.BeanDefinitionBuilder
//import org.springframework.beans.factory.support.BeanDefinitionRegistry
//import org.springframework.core.env.ConfigurableEnvironment
//import org.springframework.core.type.AnnotationMetadata
//import java.time.Duration
//
//private val logger = KotlinLogging.logger { }
//
//@Deprecated("Use --vyne.schema.publicationMethod and --vyne.schem.consumptionMethod settings to initialise schema publishers and consumers")
//object LocalStoreConfigurator: StoreConfigurator {
//    override fun configure(
//       importingClassMetadata: AnnotationMetadata,
//       registry: BeanDefinitionRegistry,
//       environment: ConfigurableEnvironment,
//       schemaSourcedProviderRegistrar: (schemaStoreClientBeanName: String) -> Unit) {
//      logger.info { "Using local schema store" }
//      registerKeepAliveMonitoringStrategies(registry, environment)
//      val schemaStoreClientBeanName =
//         registry.registerBeanDefinitionOfType(LocalValidatingSchemaStoreClient::class.java)
//      registry.registerBeanDefinitionOfType(TaxiSchemaStoreService::class.java)
//       schemaSourcedProviderRegistrar(schemaStoreClientBeanName)
//   }
//
//   private fun registerKeepAliveMonitoringStrategies(registry: BeanDefinitionRegistry, environment: ConfigurableEnvironment) {
//      val ttlCheckInSeconds = environment.getProperty(VyneRemoteSchemaStoreTllCheckInSeconds, Long::class.java) ?: 1L
//      val httpRequestTimeoutInSeconds = environment.getProperty(VyneRemoteSchemaStoreHttpRequestTimeoutInSeconds, Long::class.java) ?: 30L
//      registry.registerBeanDefinition("httpPollKeepAliveStrategyMonitor",
//         BeanDefinitionBuilder.genericBeanDefinition(HttpPollKeepAliveStrategyMonitor::class.java)
//            .addConstructorArgValue(Duration.ofSeconds(ttlCheckInSeconds))
//            .addConstructorArgValue(httpRequestTimeoutInSeconds)
//            .addConstructorArgReference(registry.registerBeanDefinitionOfType(HttpPollKeepAliveStrategyPollUrlResolver::class.java))
//            .setAutowireMode(AbstractBeanDefinition.DEPENDENCY_CHECK_ALL)
//            .beanDefinition
//      )
//
//      registry.registerBeanDefinitionOfType(NoneKeepAliveStrategyMonitor::class.java)
//   }
//}
