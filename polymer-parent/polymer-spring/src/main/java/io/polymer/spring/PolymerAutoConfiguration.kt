package io.polymer.spring

import io.osmosis.polymer.Polymer
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.osmosis.polymer.utils.log
import io.polymer.schemaStore.SchemaService
import io.polymer.schemaStore.SchemaStoreClient
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cloud.netflix.feign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.*


class PolymerFactory(private val schemaProvider: SchemaProvider) : FactoryBean<Polymer> {
   override fun isSingleton() = true
   override fun getObjectType() = Polymer::class.java

   override fun getObject(): Polymer {
      return buildPolymer()
   }

   // For readability
   fun createPolymer() = getObject()

   private fun buildPolymer(): Polymer {
      val polymer = Polymer()
      schemaProvider.schemaStrings().forEach { schema ->
         // TODO :  This is all a bit much ... going to a TaxiSchema and back again.
         // Should really be able to do:  Polymer().addSchema(TypeSchema.from(type))
         log().debug("Registering schema: $schema")
         polymer.addSchema(TaxiSchema.from(schema))
      }
      return polymer
   }
}


@Configuration
@EnableFeignClients(basePackageClasses = arrayOf(SchemaService::class))
open class PolymerAutoConfiguration {

   @Bean
   open fun schemaStoreClient(schemaService: SchemaService): SchemaStoreClient {
      return SchemaStoreClient(schemaService)
   }

   @Bean
   @ConditionalOnBean(RemoteSchemaStoreRequiredBean::class)
   open fun schemaPublisher(@Value("\${polymer.schema.name}") schemaName: String,
                            @Value("\${polymer.schema.version}") schemaVersion: String,
                            schemaStoreClient: SchemaStoreClient,
                            schemaProvider: LocalTaxiSchemaProvider
   ): LocalSchemaPublisher {
      return LocalSchemaPublisher(schemaProvider, schemaStoreClient, schemaName, schemaVersion)
   }

//   @Bean
//   @ConditionalOnBean(RemoteSchemaStoreRequiredBean::class)
//   open fun remoteSchemaProvider(schemaStoreClient: SchemaStoreClient): RemoteTaxiSchemaProvider {
//      return RemoteTaxiSchemaProvider(schemaStoreClient)
//   }

   @Bean
   open fun polymerFactory(localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                           remoteTaxiSchemaProvider: Optional<RemoteTaxiSchemaProvider>): PolymerFactory {
      val schemaProvider = if (remoteTaxiSchemaProvider.isPresent) remoteTaxiSchemaProvider.get() else localTaxiSchemaProvider
      return PolymerFactory(schemaProvider)
   }
}

// A marker bean, that we place in the context to indicate we want auto-config to
// wire up remote store
data class RemoteSchemaStoreRequiredBean(val required: Boolean = true)

//data class PolymerSchemaCandidates(val models: List<Class<*>>, val services: List<Class<*>>)
class PolymerConfigRegistrar : ImportBeanDefinitionRegistrar {
   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = importingClassMetadata.getAnnotationAttributes(EnablePolymer::class.java.name)
      importingClassMetadata.className
      val basePackageClasses = attributes["basePackageClasses"] as Array<Class<*>>
      val basePackages = basePackageClasses.map { it.`package`.name } + Class.forName(importingClassMetadata.className).`package`.name

      registry.registerBeanDefinition("localTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(LocalTaxiSchemaProvider::class.java)
         .addConstructorArgValue(scanForCandidates(basePackages, DataType::class.java))
         .addConstructorArgValue(scanForCandidates(basePackages, Service::class.java))
         .beanDefinition)

      if (attributes["useRemoteSchemaStore"] as Boolean) {
         log().debug("Enabling remote schema store")
         registry.registerBeanDefinition("RemoteTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSchemaProvider::class.java)
            .beanDefinition)
         registry.registerBeanDefinition("LocalSchemaPublisher", BeanDefinitionBuilder.genericBeanDefinition(LocalSchemaPublisher::class.java)
            .beanDefinition)
      }
   }

   fun scanForCandidates(basePackages: List<String>, annotationClass: Class<out Annotation>): List<Class<*>> {
      val scanner = ClassPathScanningCandidateComponentProvider(false)
      scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))
      return basePackages.flatMap { scanner.findCandidateComponents(it) }
         .map { beanDefinition -> Class.forName(beanDefinition.beanClassName) }
   }
}

