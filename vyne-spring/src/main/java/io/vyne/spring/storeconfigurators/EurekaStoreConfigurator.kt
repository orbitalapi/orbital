package io.vyne.spring.storeconfigurators

import io.vyne.schemaSpring.StoreConfigurator
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.eureka.EurekaClientSchemaConsumer
import io.vyne.schemaStore.eureka.EurekaClientSchemaMetaPublisher
import io.vyne.schemaStore.eureka.EurekaHttpSchemaStore
import io.vyne.spring.HttpVersionedSchemaProviderFeignConfig
import io.vyne.spring.VyneQueryServer
import io.vyne.spring.registerBeanDefinitionOfType
import io.vyne.utils.log
import mu.KotlinLogging
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy
import org.apache.http.impl.client.HttpClients
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.type.AnnotationMetadata
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger { }

@Deprecated("Use --vyne.schema.publicationMethod and --vyne.schema.consumptionMethod settings to initialise schema publishers and consumers")
object EurekaStoreConfigurator: StoreConfigurator {
   override fun configure(
      importingClassMetadata: AnnotationMetadata,
      registry: BeanDefinitionRegistry,
      environment: ConfigurableEnvironment,
      schemaSourcedProviderRegistrar: (schemaStoreClientBeanName: String) -> Unit) {
      val isVyneQueryServer = importingClassMetadata.isAnnotated(VyneQueryServer::class.java.name)
      if (!isVyneQueryServer) {
         logger.info { "Registering schema metadata to Eureka publication" }
         registry.registerBeanDefinition(
            EurekaClientSchemaMetaPublisher::class.simpleName!!,
            BeanDefinitionBuilder.genericBeanDefinition(EurekaClientSchemaMetaPublisher::class.java)
               .beanDefinition
         )

         log().info("Registering schema consumer from Http Schema Store")
         registry.registerBeanDefinitionOfType(HttpVersionedSchemaProviderFeignConfig::class.java)
         val httpSchemaStore = registry.registerBeanDefinitionOfType(EurekaHttpSchemaStore::class.java)
         schemaSourcedProviderRegistrar(httpSchemaStore)
      }

      if (isVyneQueryServer) {
         schemaSourcedProviderRegistrar("eurekaClientConsumer")
         val retryCount = environment.getProperty("vyne.taxi.rest.retry.count", Int::class.java) ?: 3
         val httpClient = HttpClients.custom()
            .setRetryHandler { _, executionCount, _ -> executionCount < retryCount }
            .setServiceUnavailableRetryStrategy(DefaultServiceUnavailableRetryStrategy(retryCount, 1000))
            .build()
         registry.registerBeanDefinition("eurekaClientConsumer",
            BeanDefinitionBuilder.genericBeanDefinition(EurekaClientSchemaConsumer::class.java)
               .addConstructorArgValue(LocalValidatingSchemaStoreClient())
               .addConstructorArgValue(RestTemplate(HttpComponentsClientHttpRequestFactory(httpClient)))
               .setAutowireMode(AbstractBeanDefinition.DEPENDENCY_CHECK_ALL)
               .beanDefinition
         )
      }
   }
}
