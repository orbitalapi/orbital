package io.vyne.spring

import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.utils.log
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.Optional

const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"

@Configuration
@AutoConfigureAfter(VyneSchemaConsumerConfigRegistrar::class, RibbonAutoConfiguration::class)
// TODO : re-enbale external schema services.
// https://gitlab.com/vyne/vyne/issues/14
//@EnableFeignClients(basePackageClasses = arrayOf(SchemaService::class))
// Don't enable Vyne if we're configuring to be a Schema Discovery service
@ConditionalOnMissingClass("io.vyne.schemaStore.TaxiSchemaService")

// If someone is only running a VyneClient,(ie @EnableVyneClient) they don't want the stuff inside this config
// If they've @EnableVynePublisher, then a LocalTaxiSchemaProvider will have been configured.
//
// MP 4-Aug-21:  Removing this ConditionalOnBean to see what breaks.
// @EnableVyneClient and @EnableVyne are hopefully sufficiently decoupled now that
// we don't need this check.
// If something is broken, document here.
//@ConditionalOnBean(LocalTaxiSchemaProvider::class)
class VyneAutoConfiguration {


}

fun BeanDefinitionRegistry.registerBeanDefinitionOfType(clazz: Class<*>): String {
   val beanName = clazz.simpleName
   this.registerBeanDefinition(
      beanName,
      BeanDefinitionBuilder.genericBeanDefinition(clazz).beanDefinition
   )
   return beanName
}
