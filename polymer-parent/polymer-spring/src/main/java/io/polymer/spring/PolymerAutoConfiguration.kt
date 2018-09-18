package io.polymer.spring

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.osmosis.polymer.Polymer
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.query.graph.operationInvocation.OperationInvoker
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.osmosis.polymer.utils.log
import io.polymer.schemaStore.*
import io.polymer.spring.invokers.RestTemplateInvoker
import io.polymer.spring.invokers.ServiceDiscoveryClientUrlResolver
import io.polymer.spring.invokers.ServiceUrlResolver
import io.polymer.spring.invokers.SpringServiceDiscoveryClient
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import lang.taxi.annotations.ServiceDiscoveryClient
import lang.taxi.generators.java.DefaultServiceMapper
import lang.taxi.generators.java.ServiceMapper
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.generators.java.extensions.HttpServiceAddressProvider
import lang.taxi.generators.java.extensions.SpringMvcHttpOperationExtension
import lang.taxi.generators.java.extensions.SpringMvcHttpServiceExtension
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.netflix.feign.EnableFeignClients
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.*
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.*


class PolymerFactory(private val schemaProvider: SchemaSourceProvider, private val operationInvokers: List<OperationInvoker>) : FactoryBean<Polymer> {
   override fun isSingleton() = true
   override fun getObjectType() = Polymer::class.java

   override fun getObject(): Polymer {
      return buildPolymer()
   }

   // For readability
   fun createPolymer() = `object`

   private fun buildPolymer(): Polymer {
      val polymer = Polymer(QueryEngineFactory.withOperationInvokers(operationInvokers))
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
@AutoConfigureAfter(PolymerConfigRegistrar::class, RibbonAutoConfiguration::class)
@EnableFeignClients(basePackageClasses = arrayOf(SchemaService::class))
// Don't enable Polymer if we're configuring to be a Schema Discovery service
@ConditionalOnMissingClass("io.polymer.schemaStore.TaxiSchemaService")
class PolymerAutoConfiguration {

//   @Bean
//   fun schemaStoreClient(schemaService: SchemaService): SchemaStoreClient {
//      return SchemaStoreClient(schemaService)
//   }

   @Bean
   fun polymerFactory(schemaProvider: SchemaSourceProvider, operationInvokers: List<OperationInvoker>): PolymerFactory {
      return PolymerFactory(schemaProvider, operationInvokers)
   }

//   @Bean fun restTemplateCustomizer(ribbonRequestFactory:RibbonClientHttpRequestFactory):RestTemplateCustomizer {
//      return object : RestTemplateCustomizer{
//         override fun customize(restTemplate: RestTemplate) {
//            restTemplate.requestFactory = ribbonRequestFactory
//         }
//      }
//   }

   // TODO : This can't be left like this, as it would effect other rest templates within
   // the target application.
   @Bean
   fun restTemplateOperationInvoker(schemaProvider: SchemaProvider,
                                    restTemplateBuilder: RestTemplateBuilder,
                                    serviceUrlResolvers: List<ServiceUrlResolver>
   ): RestTemplateInvoker {
      return RestTemplateInvoker(schemaProvider, restTemplateBuilder, serviceUrlResolvers)
   }

   @Bean
   fun serviceDiscoveryUrlResolver(discoveryClient: DiscoveryClient): ServiceDiscoveryClientUrlResolver {
      return ServiceDiscoveryClientUrlResolver(SpringServiceDiscoveryClient(discoveryClient))
   }

   @Bean
   @Primary
   fun schemaProvider(localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                      remoteTaxiSchemaProvider: Optional<RemoteTaxiSchemaProvider>): SchemaSourceProvider {
      return if (remoteTaxiSchemaProvider.isPresent) remoteTaxiSchemaProvider.get() else localTaxiSchemaProvider
   }

   @Bean("hazelcast")
   @ConditionalOnProperty("vyne.schemaStore", havingValue = "HAZELCAST")
   fun hazelcast(): HazelcastInstance {
      return Hazelcast.newHazelcastInstance()
   }


}

class PolymerConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }


   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = importingClassMetadata.getAnnotationAttributes(EnablePolymer::class.java.name)
      importingClassMetadata.className
      val basePackageClasses = attributes["basePackageClasses"] as Array<Class<*>>
      val basePackages = basePackageClasses.map { it.`package`.name } + Class.forName(importingClassMetadata.className).`package`.name

      val serviceMapper = serviceMapper(environment!!)
      val taxiGenerator = TaxiGenerator(serviceMapper = serviceMapper)

      registry.registerBeanDefinition("localTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(LocalTaxiSchemaProvider::class.java)
         .addConstructorArgValue(scanForCandidates(basePackages, DataType::class.java))
         .addConstructorArgValue(scanForCandidates(basePackages, Service::class.java))
         .addConstructorArgValue(taxiGenerator)
         .beanDefinition)

      val remoteSchemaStoreType = attributes["remoteSchemaStore"] as RemoteSchemaStoreType
      when (remoteSchemaStoreType) {
         RemoteSchemaStoreType.NONE -> log().info("Not using a remote schema store")
         RemoteSchemaStoreType.HTTP -> configureHttpSchemaStore(registry)
         RemoteSchemaStoreType.HAZELCAST -> configureHazelcastSchemaStore(registry)
      }

      if (environment!!.containsProperty("polymer.schema.name")) {
         registry.registerBeanDefinition("LocalSchemaPublisher", BeanDefinitionBuilder.genericBeanDefinition(LocalSchemaPublisher::class.java)
            .addConstructorArgValue(environment!!.getProperty("polymer.schema.name"))
            .addConstructorArgValue(environment!!.getProperty("polymer.schema.version"))
            .beanDefinition
         )
      } else {
         log().warn("Polymer is enabled, but no schema name is defined.  This application is not publishing any schemas.  If it should be, define polymer.schema.name & polymer.schema.version")
      }
   }

   private fun registerRemoteSchemaProvider(registry: BeanDefinitionRegistry, schemaStoreClientBeanName: String) {
      log().debug("Enabling remote schema store")
      registry.registerBeanDefinition("RemoteTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSchemaProvider::class.java)
         .addConstructorArgReference(schemaStoreClientBeanName)
         .beanDefinition)
   }

   private fun configureHazelcastSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using a Hazelcast based schema store")
      val schemaStoreClientBeanName = HazelcastSchemaStoreClient::class.simpleName!!
      registry.registerBeanDefinition(schemaStoreClientBeanName,
         BeanDefinitionBuilder.genericBeanDefinition(HazelcastSchemaStoreClient::class.java)
            .addConstructorArgReference("hazelcast")
            .addConstructorArgValue(TaxiSchemaValidator())
            .beanDefinition
      )
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)

//      registry.registerBeanDefinitionOfType(HazelcastSchemaStoreClient::class.java)
      environment!!.propertySources.addLast(MapPropertySource("VyneHazelcastProperties", mapOf("vyne.schemaStore" to RemoteSchemaStoreType.HAZELCAST.name)))
   }

   private fun configureHttpSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using an Http based schema store")
      val schemaStoreClientBeanName = registry.registerBeanDefinitionOfType(HttpSchemaStoreClient::class.java)
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)
   }

   fun serviceMapper(env: Environment): ServiceMapper {
      val applicationName = env.getProperty("spring.application.name")
         ?: error("Currently, only service-discovery enabled services are supported.  Please define spring.application.name in properties")
      val operationExtensions = listOf(SpringMvcHttpOperationExtension())
      val serviceExtensions = listOf(SpringMvcHttpServiceExtension(
         object : HttpServiceAddressProvider {
            override fun toAnnotation(): lang.taxi.types.Annotation {
               return ServiceDiscoveryClient(applicationName).toAnnotation()
            }
         }
      ))
      return DefaultServiceMapper(operationExtensions = operationExtensions, serviceExtensions = serviceExtensions)
   }

   fun scanForCandidates(basePackages: List<String>, annotationClass: Class<out Annotation>): List<Class<*>> {
      val scanner = ClassPathScanningCandidateComponentProvider(false)
      scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))
      return basePackages.flatMap { scanner.findCandidateComponents(it) }
         .map { beanDefinition -> Class.forName(beanDefinition.beanClassName) }
   }
}

fun BeanDefinitionRegistry.registerBeanDefinitionOfType(clazz: Class<*>): String {
   val beanName = clazz.simpleName
   this.registerBeanDefinition(beanName,
      BeanDefinitionBuilder.genericBeanDefinition(clazz).beanDefinition)
   return beanName
}
