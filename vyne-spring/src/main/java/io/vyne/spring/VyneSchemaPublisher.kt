package io.vyne.spring

import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import lang.taxi.generators.java.DefaultServiceMapper
import lang.taxi.generators.java.ServiceMapper
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.generators.java.extensions.ServiceDiscoveryAddressProvider
import lang.taxi.generators.java.extensions.SpringMvcHttpOperationExtension
import lang.taxi.generators.java.extensions.SpringMvcHttpServiceExtension
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.convert.ConversionService
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import kotlin.reflect.KClass

enum class SchemaPublicationMethod {
   /**
    * Turns off schema publication
    */
   DISABLED,

   /**
    * Publish schema to local query server.
    */
   LOCAL,

   /**
    * Publish schemas to a remote query server, and execute queries there
    */
   REMOTE,

   /**
    * Publish metadata about this schema to Eureka, and let Vyne fetch on demand
    */
   EUREKA,

   /**
    * Use a distributed mesh of schemas, and execute queries locally.
    * Enterprise only.
    */
   DISTRIBUTED
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneSchemaPublisherConfigRegistrar::class, VyneSpringConfig::class)
annotation class VyneSchemaPublisher(val basePackageClasses: Array<KClass<out Any>> = [],
                                     val publicationMethod: SchemaPublicationMethod = SchemaPublicationMethod.REMOTE,
                                     val schemaFile: String = ""
)


class VyneSchemaPublisherConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name)

      val basePackageClasses = attributes["basePackageClasses"] as Array<Class<*>>
      val schemaFileInClassPath = attributes["schemaFile"] as String

      val basePackages =
         basePackageClasses.map { it.`package`.name } + Class.forName(importingClassMetadata.className).`package`.name

      val serviceMapper = serviceMapper(environment!!)
      val taxiGenerator = TaxiGenerator(serviceMapper = serviceMapper)

      registerLocalSchemaPublication(schemaFileInClassPath, registry, basePackages, taxiGenerator)
   }

   private fun serviceMapper(env: Environment): ServiceMapper {
      val applicationName = env.getProperty("spring.application.name")
         ?: error("Currently, only service-discovery enabled services are supported.  Please define spring.application.name in properties")

      val contextPath = env.getProperty("server.servlet.context-path")?:""
      val operationExtensions = listOf(SpringMvcHttpOperationExtension(contextPath))
      val serviceExtensions = listOf(
         SpringMvcHttpServiceExtension(
         ServiceDiscoveryAddressProvider(applicationName)
         )
      )
      return DefaultServiceMapper(operationExtensions = operationExtensions, serviceExtensions = serviceExtensions)
   }

   private fun registerLocalSchemaPublication(
      schemaFileInClassPath: String,
      registry: BeanDefinitionRegistry,
      basePackages: List<String>,
      taxiGenerator: TaxiGenerator
   ) {
      if (schemaFileInClassPath.isBlank()) {
         registry.registerBeanDefinition(
            "localTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(LocalTaxiSchemaProvider::class.java)
               .addConstructorArgValue(scanForCandidates(basePackages, DataType::class.java))
               .addConstructorArgValue(scanForCandidates(basePackages, Service::class.java))
               .addConstructorArgValue(taxiGenerator)
               .addConstructorArgValue(null)
               .beanDefinition
         )
      } else {
         registry.registerBeanDefinition(
            "localTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(LocalTaxiSchemaProvider::class.java)
               .addConstructorArgValue(scanForCandidates(basePackages, DataType::class.java))
               .addConstructorArgValue(scanForCandidates(basePackages, Service::class.java))
               .addConstructorArgValue(taxiGenerator)
               .addConstructorArgValue(ClassPathSchemaSourceProvider(schemaFileInClassPath))
               .beanDefinition
         )
      }
   }

   private fun scanForCandidates(basePackages: List<String>, annotationClass: Class<out Annotation>): List<Class<*>> {
      val scanner = ClassPathScanningCandidateComponentProvider(false)
      scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))
      return basePackages.flatMap { scanner.findCandidateComponents(it) }
         .map { beanDefinition -> Class.forName(beanDefinition.beanClassName) }
   }

}

class VyneSpringConfig {
   // Required to support parsing of default durations in HttpSchemaStoreClient.
   // Not needed after Spring Boot 2.1:
   // https://stackoverflow.com/questions/51818137/spring-boot-2-converting-duration-java-8-application-properties/51823308
   @Bean
   @ConditionalOnMissingBean(ConversionService::class)
   fun conversionService(): ConversionService {
      return ApplicationConversionService.getSharedInstance()
   }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class, VyneSchemaConsumerConfigRegistrar::class)
annotation class VyneQueryServer

