package io.vyne.schemaSpring

import io.vyne.schemaPublisherApi.loaders.SchemaSourcesLoader
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import lang.taxi.generators.java.DefaultServiceMapper
import lang.taxi.generators.java.ServiceMapper
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.generators.java.extensions.ServiceDiscoveryAddressProvider
import lang.taxi.generators.java.extensions.SpringMvcHttpOperationExtension
import lang.taxi.generators.java.extensions.SpringMvcHttpServiceExtension
import mu.KotlinLogging
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import kotlin.reflect.KClass

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
   fun registerSchemaSourceProvider(
      registry: BeanDefinitionRegistry,
      schemaStoreClientBeanName: String,
      importingClassMetadata: AnnotationMetadata,
      environment: ConfigurableEnvironment,
      vyneSchemaPublisherAttributes: Map<String, Any>
   ) {
      val localTaxiSchemaSourceProvider = if (vyneSchemaPublisherAttributes.isNotEmpty()) {
         val basePackageClasses = vyneSchemaPublisherAttributes["basePackageClasses"] as Array<Class<*>>
         // Note: This is purely for annotation driven config of apps publishing a schema.
         // It's not the same thing as what our schema server app uses.
         val schemaFileInClassPath = vyneSchemaPublisherAttributes["schemaFile"] as String
         val projectPath = vyneSchemaPublisherAttributes["projectPath"] as String
         val basePackages =
            basePackageClasses.map { it.`package`.name } + Class.forName(importingClassMetadata.className).`package`.name
         val serviceMapper = serviceMapper(environment)
         val taxiGenerator = TaxiGenerator(serviceMapper = serviceMapper)
         val schemaSourcesLoader: Class<out SchemaSourcesLoader> =
            vyneSchemaPublisherAttributes["sourcesLoader"] as Class<out SchemaSourcesLoader>
         val projects = vyneSchemaPublisherAttributes["projects"] as? Array<AnnotationAttributes> ?: emptyArray()
         tryGetLocalTaxiSchemaProvider(
            schemaFileInClassPath,
            projectPath,
            basePackages,
            taxiGenerator,
            environment,
            schemaSourcesLoader,
            projects.toList()
         )
      } else {
         null
      }

      if (localTaxiSchemaSourceProvider != null) {
         logger.info { "Enabling local taxi schema source provider" }
         registry.registerBeanDefinition("localTaxiSchemaProvider", localTaxiSchemaSourceProvider)
      } else {
         logger.info { "Enabling remote taxi schema source provider" }
         registry.registerBeanDefinition(
            "RemoteTaxiSchemaProvider",
            BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
               //.addConstructorArgReference(schemaStoreClientBeanName)
               .beanDefinition
         )
      }
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

   private fun tryGetLocalTaxiSchemaProvider(
      schemaFileLocation: String,
      projectPath: String,
      basePackages: List<String>,
      taxiGenerator: TaxiGenerator,
      environment: ConfigurableEnvironment,
      schemaLoader: Class<out SchemaSourcesLoader>,
      projects: List<AnnotationAttributes>
   ): AbstractBeanDefinition? {
      return when {
         projects.isNotEmpty() -> {
            val loadableProjects = projects.map { projectAttributes ->
               LoadableSchemaProject(
                  projectAttributes.getString("projectPath"),
                  projectAttributes.getClass("sourcesLoader")
               )
            }
            logger.info { "Loading multiple schema projects:  $loadableProjects" }
            return BeanDefinitionBuilder.genericBeanDefinition(ProjectPathSchemaSourceProvider::class.java)
               .addConstructorArgValue(loadableProjects)
               .addConstructorArgValue(environment)
               .beanDefinition
         }
         
         projectPath.isBlank() && schemaFileLocation.isBlank() -> {
            val dataTypes = scanForCandidates(basePackages, DataType::class.java)
            val services = scanForCandidates(basePackages, Service::class.java)
            logger.info { "Generating taxi schema from annotations.  Found ${dataTypes.size} data types and ${services.size} services as candidates" }
            if (dataTypes.isNotEmpty() || services.isNotEmpty()) {
               return BeanDefinitionBuilder.genericBeanDefinition(AnnotationCodeGeneratingSchemaProvider::class.java)
                  .addConstructorArgValue(dataTypes)
                  .addConstructorArgValue(services)
                  .addConstructorArgValue(taxiGenerator)
                  .beanDefinition
            } else {
               null
            }
         }



         projectPath.isNotBlank() -> {
            logger.info { "Using a project path based schema source provider, from projectPath value $projectPath" }
            val schemaProject = LoadableSchemaProject(projectPath, schemaLoader)
            return BeanDefinitionBuilder.genericBeanDefinition(ProjectPathSchemaSourceProvider::class.java)
               .addConstructorArgValue(listOf(schemaProject))
               .addConstructorArgValue(environment)
               .beanDefinition
         }

         schemaFileLocation.isNotBlank() -> {
            logger.info { "Using a file based schema source provider, from source at $schemaFileLocation" }
            return BeanDefinitionBuilder.genericBeanDefinition(FileBasedSchemaSourceProvider::class.java)
               .addConstructorArgValue(schemaFileLocation)
               .beanDefinition
         }

         else -> null
      }
   }

   private fun scanForCandidates(basePackages: List<String>, annotationClass: Class<out Annotation>): List<Class<*>> {
      val scanner = ClassPathScanningCandidateComponentProvider(false)
      scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))
      return basePackages.flatMap { scanner.findCandidateComponents(it) }
         .map { beanDefinition -> Class.forName(beanDefinition.beanClassName) }
   }
}

