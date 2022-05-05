package io.vyne.schema.spring

import mu.KotlinLogging
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.type.AnnotationMetadata

val logger = KotlinLogging.logger { }

interface StoreConfigurator {
   fun configure(
      importingClassMetadata: AnnotationMetadata,
      registry: BeanDefinitionRegistry,
      environment: ConfigurableEnvironment,
      schemaSourcedProviderRegistrar: (schemaStoreClientBeanName: String) -> Unit
   ) {
   }
}

object DisabledStoreConfigurator : StoreConfigurator

object SchemaSourceProviderRegistrar {

}

