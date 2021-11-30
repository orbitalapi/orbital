package io.vyne.spring

import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.utils.log
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import lang.taxi.generators.java.DefaultServiceMapper
import lang.taxi.generators.java.ServiceMapper
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.generators.java.extensions.ServiceDiscoveryAddressProvider
import lang.taxi.generators.java.extensions.SpringMvcHttpOperationExtension
import lang.taxi.generators.java.extensions.SpringMvcHttpServiceExtension
import mu.KotlinLogging
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.context.annotation.Primary
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.Optional
import kotlin.reflect.KClass

const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"

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

/**
 * A service which publishes Vyne Schemas to another component.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
   VyneSchemaStoreConfigRegistrar::class,
   VyneSchemaPublisherConfigRegistrar::class,
   SchemaSourcePrimaryBeanConfig::class,
)
annotation class VyneSchemaPublisher(
   val basePackageClasses: Array<KClass<out Any>> = [],
   val publicationMethod: SchemaPublicationMethod = SchemaPublicationMethod.REMOTE,
   @Deprecated("use projectPath")
   val schemaFile: String = "",
   val projectPath: String = ""
)


class VyneSchemaPublisherConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private val logger = KotlinLogging.logger {}
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }

   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name)

      val basePackageClasses = attributes["basePackageClasses"] as Array<Class<*>>
      // Note: This is purely for annotation driven config of apps publishing a schema.
      // It's not the same thing as what our schema server app uses.
      val schemaFileInClassPath = attributes["schemaFile"] as String
      val projectPath = attributes["projectPath"] as String

      val basePackages =
         basePackageClasses.map { it.`package`.name } + Class.forName(importingClassMetadata.className).`package`.name

      val serviceMapper = serviceMapper(environment!!)
      val taxiGenerator = TaxiGenerator(serviceMapper = serviceMapper)

      registerLocalSchemaPublication(schemaFileInClassPath, projectPath, registry, basePackages, taxiGenerator)
   }

   private fun serviceMapper(env: Environment): ServiceMapper {
      val applicationName = env.getProperty("spring.application.name")
         ?: error("Currently, only service-discovery enabled services are supported.  Please define spring.application.name in properties")

      val contextPath = env.getProperty("server.servlet.context-path") ?: ""
      val operationExtensions = listOf(SpringMvcHttpOperationExtension(contextPath))
      val serviceExtensions = listOf(
         SpringMvcHttpServiceExtension(
            ServiceDiscoveryAddressProvider(applicationName)
         )
      )
      return DefaultServiceMapper(operationExtensions = operationExtensions, serviceExtensions = serviceExtensions)
   }

   private fun registerLocalSchemaPublication(
      schemaFileLocation: String,
      projectPath: String,
      registry: BeanDefinitionRegistry,
      basePackages: List<String>,
      taxiGenerator: TaxiGenerator
   ) {
      when {
         projectPath.isBlank() && schemaFileLocation.isBlank() -> {
            val dataTypes = scanForCandidates(basePackages, DataType::class.java)
            val services = scanForCandidates(basePackages, Service::class.java)
            logger.info { "Generating taxi schema from annotations.  Found ${dataTypes.size} data types and ${services.size} services as candidates" }
            registry.registerBeanDefinition(
               "localTaxiSchemaProvider",
               BeanDefinitionBuilder.genericBeanDefinition(AnnotationCodeGeneratingSchemaProvider::class.java)
                  .addConstructorArgValue(dataTypes)
                  .addConstructorArgValue(services)
                  .addConstructorArgValue(taxiGenerator)
                  .beanDefinition
            )
         }

         projectPath.isNotBlank() -> {
            logger.info { "Using a project path based schema source provider, from projectPath value $projectPath" }
            registry.registerBeanDefinition(
               "localTaxiSchemaProvider",
               BeanDefinitionBuilder.genericBeanDefinition(ProjectPathSchemaSourceProvider::class.java)
                  .addConstructorArgValue(projectPath)
                  .addConstructorArgValue(environment!!)
                  .beanDefinition
            )
         }

         schemaFileLocation.isNotBlank() -> {
            logger.info { "Using a file based schema source provider, from source at $schemaFileLocation" }
            registry.registerBeanDefinition(
               "localTaxiSchemaProvider",
               BeanDefinitionBuilder.genericBeanDefinition(FileBasedSchemaSourceProvider::class.java)
                  .addConstructorArgValue(schemaFileLocation)
                  .beanDefinition
            )
         }
      }
   }

   private fun scanForCandidates(basePackages: List<String>, annotationClass: Class<out Annotation>): List<Class<*>> {
      val scanner = ClassPathScanningCandidateComponentProvider(false)
      scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))
      return basePackages.flatMap { scanner.findCandidateComponents(it) }
         .map { beanDefinition -> Class.forName(beanDefinition.beanClassName) }
   }

}

/**
 * A class which simply works around having
 * multiple SchemaSourceProivders exposed, and
 * resolves to a primary.
 *
 * This is a smell, as we need to prevent having
 * multiple SchemaSourceProviders.  Until we fix that,
 * we live with this.
 *
 */
class SchemaSourcePrimaryBeanConfig {
   // Required to support parsing of default durations in HttpSchemaStoreClient.
   // Not needed after Spring Boot 2.1:
   // https://stackoverflow.com/questions/51818137/spring-boot-2-converting-duration-java-8-application-properties/51823308
//   18-Sept:  Commenting this out, since we're now
//   on Spring Boot 2.3
//   If this breaks something, then we need to add it back in, but
//   it's currently exposed in a really obscure place, so move this elsewhere.
//   Note: This class USED to be called VyneSpringConfig
//   @Bean
//   @ConditionalOnMissingBean(ConversionService::class)
//   fun conversionService(): ConversionService {
//      return ApplicationConversionService.getSharedInstance()
//   }


   @Bean
   @Primary // Need to understand why it'd be valid for there to be multiple of these
   fun schemaProvider(
      localTaxiSchemaProvider: Optional<InternalSchemaSourceProvider>,
      remoteTaxiSchemaProvider: Optional<RemoteTaxiSourceProvider>
   ): SchemaSourceProvider {
      return when {
         // 23-Aug: Ordering seems wrong.  If there's a localSchemaProvider (ie., generated from
         // code), then we wanna use it, don't we?
         // 18-Sep: That makes sense, but it breaks stuff in places like Cask, where we
         // generate code, but not through annotations.
         localTaxiSchemaProvider.isPresent && localTaxiSchemaProvider.get().schemaStrings().isNotEmpty() -> localTaxiSchemaProvider.get()
         remoteTaxiSchemaProvider.isPresent -> remoteTaxiSchemaProvider.get()



         else -> {
            log().warn("No schema provider (either local or remote).  Using an empty schema provider")
            SimpleTaxiSchemaProvider("")
         }
      }
   }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(EnableVyneConfiguration::class, VyneSchemaConsumerConfigRegistrar::class)
annotation class VyneQueryServer

