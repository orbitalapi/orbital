package io.vyne.spring

import io.vyne.schema.spring.SchemaSourceProviderRegistrar
import mu.KotlinLogging
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata

/**
 * Wires up a schema store, but does not configure a local schema publisher.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneSchemaStoreConfigRegistrar::class)
annotation class EnableVyneSchemaStore

private val logger = KotlinLogging.logger { }

/**
 * Config which wires up the schema store, based on the defined schema
 * distribution method.
 *
 * Both schema consumers and schema publishers need access to a schema store,
 * so it's valid for either of those two configs to import this.
 */
class VyneSchemaStoreConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private lateinit var environment: ConfigurableEnvironment
   private var importingClassMetadata: AnnotationMetadata? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      this.importingClassMetadata = importingClassMetadata
      val vyneSchemaPublisherAttributes = vyneSchemaPublisherAttributes(importingClassMetadata)
      val vyneSchemaConsumerAttributes = vyneSchemaConsumerAttributes(importingClassMetadata)
      val attributes = vyneSchemaConsumerAttributes + vyneSchemaPublisherAttributes
      val isVyneQueryServer = importingClassMetadata.isAnnotated(VyneQueryServer::class.java.name)
      val annotationPublicationMethod = attributes["publicationMethod"] as? SchemaPublicationMethod
      val publicationMethod: String? =
         environment.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD, annotationPublicationMethod?.name)

      if (publicationMethod == null) {
         logger.warn("There is no schema distribution mechanism configured. Vyne will not fetch schemas. Consider setting $VYNE_SCHEMA_PUBLICATION_METHOD, or using an annotation based setting")
      } else {
         logger.info { "${VYNE_SCHEMA_PUBLICATION_METHOD}=${publicationMethod}" }
         when (val schemaPublicationMethod = SchemaPublicationMethod.valueOf(publicationMethod)) {
            SchemaPublicationMethod.EUREKA -> schemaPublicationMethod
               .storeConfigurator()
               .configure(importingClassMetadata, registry, environment) { schemaStoreClientBeanName ->
                  if (isVyneQueryServer) {
                     SchemaSourceProviderRegistrar.registerSchemaSourceProvider(
                        registry,
                        importingClassMetadata,
                        environment,
                        emptyMap()
                     )
                  } else {
                     SchemaSourceProviderRegistrar.registerSchemaSourceProvider(
                        registry,
                        importingClassMetadata,
                        environment,
                        vyneSchemaPublisherAttributes
                     )
                  }

               }
            SchemaPublicationMethod.LOCAL -> schemaPublicationMethod.storeConfigurator()
               .configure(importingClassMetadata, registry, environment) {}
            else -> schemaPublicationMethod.storeConfigurator()
               .configure(importingClassMetadata, registry, environment) { schemaStoreClientBeanName ->
                  SchemaSourceProviderRegistrar.registerSchemaSourceProvider(
                     registry,
                     importingClassMetadata,
                     environment,
                     vyneSchemaPublisherAttributes
                  )
               }
         }
      }

      if (publicationMethod != null && environment.containsProperty("vyne.schema.name")) {
         registry.registerBeanDefinition(
            "LocalSchemaPublisher", BeanDefinitionBuilder.genericBeanDefinition(LocalSchemaPublisher::class.java)
               .addConstructorArgValue(environment.getProperty("vyne.schema.name"))
               .addConstructorArgValue(environment.getProperty("vyne.schema.version"))
               .beanDefinition
         )
      } else {
         logger.warn("No schema name provided, so publication of auto-detected schemas (either annotation driven, or classpath driven) is disabled.  If this is incorrect, define vyne.schema.name & vyne.schema.version")
      }
   }

   private fun vyneSchemaPublisherAttributes(importingClassMetadata: AnnotationMetadata) =
      importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name) ?: emptyMap()

   private fun vyneSchemaConsumerAttributes(importingClassMetadata: AnnotationMetadata) =
      importingClassMetadata.getAnnotationAttributes(VyneSchemaConsumer::class.java.name) ?: emptyMap()
}
