package io.vyne.spring.storeconfigurators

import com.hazelcast.core.HazelcastInstance
import io.vyne.schemaSpring.StoreConfigurator
import io.vyne.schemaStore.HazelcastSchemaStoreClient
import io.vyne.schemaStore.TaxiSchemaValidator
import io.vyne.spring.SchemaPublicationMethod
import io.vyne.spring.VYNE_SCHEMA_PUBLICATION_METHOD
import io.vyne.spring.VyneHazelcastConfig
import mu.KotlinLogging
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.AnnotationMetadata

private val logger = KotlinLogging.logger { }

@Deprecated("Use --vyne.schema.publicationMethod and --vyne.schem.consumptionMethod settings to initialise schema publishers and consumers")
object HazelcastStoreConfigurator: StoreConfigurator {
   override fun configure(
      importingClassMetadata: AnnotationMetadata,
      registry: BeanDefinitionRegistry,
      environment: ConfigurableEnvironment,
      schemaSourcedProviderRegistrar: (schemaStoreClientBeanName: String) -> Unit) {
      logger.info { "Using a Hazelcast based schema store" }
      val schemaStoreClientBeanName = HazelcastSchemaStoreClient::class.simpleName!!
      registry.registerBeanDefinition(
         schemaStoreClientBeanName,
         BeanDefinitionBuilder.genericBeanDefinition(HazelcastSchemaStoreClient::class.java)
            .addConstructorArgReference("hazelcast")
            .addConstructorArgValue(TaxiSchemaValidator())
            .beanDefinition
      )

      registry.registerBeanDefinition("hazelcast",
         BeanDefinitionBuilder.genericBeanDefinition(HazelcastInstance::class.java) {
            logger.info("Registering new Hazelcast instance for schema discovery")
            VyneHazelcastConfig.schemaStoreHazelcastInstance()
         }
            .beanDefinition)


      schemaSourcedProviderRegistrar(schemaStoreClientBeanName)
      environment.propertySources.addLast(
         MapPropertySource(
            "VyneHazelcastProperties",
            mapOf(VYNE_SCHEMA_PUBLICATION_METHOD to SchemaPublicationMethod.DISTRIBUTED.name)
         )
      )
   }
}
