package io.vyne.spring

import io.vyne.schemaStore.HazelcastSchemaStoreClient
import io.vyne.schemaStore.HttpSchemaStoreClient
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.TaxiSchemaStoreService
import io.vyne.schemaStore.TaxiSchemaValidator
import io.vyne.schemaStore.eureka.EurekaClientSchemaMetaPublisher
import io.vyne.utils.log
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.AnnotationMetadata

/**
 * Config which wires up various schema consumers, based on the defined method.
 */
class VyneSchemaConsumerConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }


   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = collectAnnotationAttributes(importingClassMetadata)

      val isVyneQueryServer = importingClassMetadata.isAnnotated(VyneQueryServer::class.java.name)
      val annotationPublicationMethod = attributes["publicationMethod"] as SchemaPublicationMethod
      val publicationMethod =
         environment!!.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD, annotationPublicationMethod.name)

      log().info("${VYNE_SCHEMA_PUBLICATION_METHOD}=${publicationMethod}")

      when (SchemaPublicationMethod.valueOf(publicationMethod)) {
         //SchemaPublicationMethod.DISABLED -> log().info("Not using a remote schema store")
         SchemaPublicationMethod.LOCAL -> configureLocalSchemaStore(registry)
         SchemaPublicationMethod.REMOTE -> configureHttpSchemaStore(registry)
         SchemaPublicationMethod.EUREKA -> configureEurekaSchemaStore(registry, isVyneQueryServer)
         SchemaPublicationMethod.DISTRIBUTED -> configureHazelcastSchemaStore(registry)
      }

      if (environment!!.containsProperty("vyne.schema.name")) {
         registry.registerBeanDefinition(
            "LocalSchemaPublisher", BeanDefinitionBuilder.genericBeanDefinition(LocalSchemaPublisher::class.java)
               .addConstructorArgValue(environment!!.getProperty("vyne.schema.name"))
               .addConstructorArgValue(environment!!.getProperty("vyne.schema.version"))
               .beanDefinition
         )
      } else {
         log().warn("Vyne is enabled, but no schema name is defined.  This application is not publishing any schemas.  If it should be, define vyne.schema.name & vyne.schema.version")
      }
   }

   private fun collectAnnotationAttributes(importingClassMetadata: AnnotationMetadata): Map<String, Any> {
      val vyneSchemaPublisherAnnotationAttributes = importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name) ?: emptyMap()
      val vyneSchemaConsumerAnnotationAttributes = importingClassMetadata.getAnnotationAttributes(VyneSchemaConsumer::class.java.name) ?: emptyMap()
      return vyneSchemaPublisherAnnotationAttributes + vyneSchemaConsumerAnnotationAttributes

   }


   private fun configureEurekaSchemaStore(registry: BeanDefinitionRegistry, isVyneQueryServer: Boolean) {
      log().debug("Enabling Eureka based schema store")
      if (!isVyneQueryServer) {
         registry.registerBeanDefinition(
            EurekaClientSchemaMetaPublisher::class.simpleName!!,
            BeanDefinitionBuilder.genericBeanDefinition(EurekaClientSchemaMetaPublisher::class.java)
               .beanDefinition
         )
      }

      if (isVyneQueryServer) {
         registry.registerBeanDefinition(
            "RemoteTaxiSchemaProvider",
            BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
               .addConstructorArgReference("eurekaClientConsumer")
               .beanDefinition
         )
      }
   }

   private fun registerRemoteSchemaProvider(registry: BeanDefinitionRegistry, schemaStoreClientBeanName: String) {
      log().debug("Enabling remote schema store")
      registry.registerBeanDefinition(
         "RemoteTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
            .addConstructorArgReference(schemaStoreClientBeanName)
            .beanDefinition
      )
   }

   private fun configureHazelcastSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using a Hazelcast based schema store")
      val schemaStoreClientBeanName = HazelcastSchemaStoreClient::class.simpleName!!
      registry.registerBeanDefinition(
         schemaStoreClientBeanName,
         BeanDefinitionBuilder.genericBeanDefinition(HazelcastSchemaStoreClient::class.java)
            .addConstructorArgReference("hazelcast")
            .addConstructorArgValue(TaxiSchemaValidator())
            .beanDefinition
      )
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)

      environment!!.propertySources.addLast(
         MapPropertySource(
            "VyneHazelcastProperties",
            mapOf(VYNE_SCHEMA_PUBLICATION_METHOD to SchemaPublicationMethod.DISTRIBUTED.name)
         )
      )
   }

   private fun configureLocalSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using local schema store")
      val schemaStoreClientBeanName =
         registry.registerBeanDefinitionOfType(LocalValidatingSchemaStoreClient::class.java)
      registry.registerBeanDefinitionOfType(TaxiSchemaStoreService::class.java)
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)
   }

   private fun configureHttpSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using an Http based schema store")
      registry.registerBeanDefinitionOfType(HttpSchemaStoreFeignConfig::class.java)
      val schemaStoreClientBeanName = registry.registerBeanDefinitionOfType(HttpSchemaStoreClient::class.java)
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)
   }

}
