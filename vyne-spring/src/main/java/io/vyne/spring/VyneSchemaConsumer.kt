package io.vyne.spring

import io.vyne.schemaStore.HttpSchemaStore
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneSchemaConsumerRegistrar::class)
annotation class VyneSchemaConsumer(
   val publicationMethod: SchemaPublicationMethod = SchemaPublicationMethod.REMOTE
)


class VyneSchemaConsumerRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null

   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      // TODO introduce schema consumption method.
      val method = environment!!.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD, SchemaPublicationMethod.EUREKA.name)
      if (method == SchemaPublicationMethod.EUREKA.name) {
         registry.registerBeanDefinitionOfType(HttpVersionedSchemaProviderFeignConfig::class.java)
         val httpSchemaStore = registry.registerBeanDefinitionOfType(HttpSchemaStore::class.java)

         registry.registerBeanDefinition("RemoteTaxiSchemaProvider",
            BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
               .addConstructorArgReference(httpSchemaStore)
               .beanDefinition
         )
      }
   }
}

