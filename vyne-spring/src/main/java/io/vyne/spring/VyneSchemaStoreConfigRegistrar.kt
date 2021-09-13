package io.vyne.spring

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.vyne.schemaStore.HazelcastSchemaStoreClient
import io.vyne.schemaStore.HttpSchemaStore
import io.vyne.schemaStore.HttpSchemaStoreClient
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.TaxiSchemaStoreService
import io.vyne.schemaStore.TaxiSchemaValidator
import io.vyne.schemaStore.eureka.EurekaClientSchemaMetaPublisher
import io.vyne.utils.log
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.AnnotationMetadata

/**
 * Wires up a schema store, but does not configure a local schema publisher.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   VyneSpringConfig::class,
   VyneSchemaStoreConfigRegistrar::class
)
annotation class EnableVyneSchemaStore

/**
 * Config which wires up the schema store, based on the defined schema
 * distribution method.
 *
 * Both schema consumers and schema publishers need access to a schema store,
 * so it's valid for either of those two configs to import this.
 */
class VyneSchemaStoreConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = collectAnnotationAttributes(importingClassMetadata)

      val isVyneQueryServer = importingClassMetadata.isAnnotated(VyneQueryServer::class.java.name)
      val annotationPublicationMethod = attributes["publicationMethod"] as SchemaPublicationMethod?
      val publicationMethod: String? =
         environment!!.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD, annotationPublicationMethod?.name)

      if (publicationMethod == null) {
         log().warn("There is no schema distribution mechanism configured.  Vyne will not fetch schemas. Consider setting $VYNE_SCHEMA_PUBLICATION_METHOD, or using an annotation based setting")
      } else {
         log().info("${VYNE_SCHEMA_PUBLICATION_METHOD}=${publicationMethod}")

         when (SchemaPublicationMethod.valueOf(publicationMethod)) {
            //SchemaPublicationMethod.DISABLED -> log().info("Not using a remote schema store")
            SchemaPublicationMethod.LOCAL -> configureLocalSchemaStore(registry)
            SchemaPublicationMethod.REMOTE -> configureHttpSchemaStore(registry)
            SchemaPublicationMethod.EUREKA -> configureEurekaSchemaStore(registry, isVyneQueryServer)
            SchemaPublicationMethod.DISTRIBUTED -> configureHazelcastSchemaStore(registry)
         }
      }


      if (environment!!.containsProperty("vyne.schema.name")) {
         registry.registerBeanDefinition(
            "LocalSchemaPublisher", BeanDefinitionBuilder.genericBeanDefinition(LocalSchemaPublisher::class.java)
               .addConstructorArgValue(environment!!.getProperty("vyne.schema.name"))
               .addConstructorArgValue(environment!!.getProperty("vyne.schema.version"))
               .beanDefinition
         )
      } else {
         log().warn("No schema name provided, so publication of auto-detected schemas (either annotation driven, or classpath driven) is disabled.  If this is incorrect, define vyne.schema.name & vyne.schema.version")
      }
   }

   private fun configureEurekaSchemaStore(registry: BeanDefinitionRegistry, isVyneQueryServer: Boolean) {
      if (!isVyneQueryServer) {
         log().info("Registering schema metadata to Eureka publication")
         registry.registerBeanDefinition(
            EurekaClientSchemaMetaPublisher::class.simpleName!!,
            BeanDefinitionBuilder.genericBeanDefinition(EurekaClientSchemaMetaPublisher::class.java)
               .beanDefinition
         )

         log().info("Registering schema consumer from Http Schema Store")
         registry.registerBeanDefinitionOfType(HttpVersionedSchemaProviderFeignConfig::class.java)
         val httpSchemaStore = registry.registerBeanDefinitionOfType(HttpSchemaStore::class.java)

         registry.registerBeanDefinition("RemoteTaxiSchemaProvider",
            BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
               .addConstructorArgReference(httpSchemaStore)
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
      log().info("Enabling remote schema store")
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

      registry.registerBeanDefinition("hazelcast",
         BeanDefinitionBuilder.genericBeanDefinition(HazelcastInstance::class.java) {
            log().info("Registering new Hazelcast instance for schema discovery")
            Hazelcast.newHazelcastInstance()
         }
            .beanDefinition)


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

   private fun collectAnnotationAttributes(importingClassMetadata: AnnotationMetadata): Map<String, Any> {
      val vyneSchemaPublisherAnnotationAttributes =
         importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name) ?: emptyMap()
      val vyneSchemaConsumerAnnotationAttributes =
         importingClassMetadata.getAnnotationAttributes(VyneSchemaConsumer::class.java.name) ?: emptyMap()
      return vyneSchemaPublisherAnnotationAttributes + vyneSchemaConsumerAnnotationAttributes
   }


}
