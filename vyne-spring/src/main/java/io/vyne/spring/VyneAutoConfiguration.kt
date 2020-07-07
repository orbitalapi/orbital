package io.vyne.spring

import com.hazelcast.config.Config
import com.hazelcast.config.DiscoveryStrategyConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.instance.AddressPicker
import com.hazelcast.instance.DefaultNodeContext
import com.hazelcast.instance.HazelcastInstanceFactory
import com.hazelcast.instance.Node
import com.hazelcast.logging.Slf4jFactory
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.HazelcastSchemaStoreClient
import io.vyne.schemaStore.HttpSchemaStoreClient
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.TaxiSchemaValidator
import io.vyne.spring.invokers.AbsoluteUrlResolver
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.invokers.ServiceDiscoveryClientUrlResolver
import io.vyne.spring.invokers.ServiceUrlResolver
import io.vyne.spring.invokers.SpringServiceDiscoveryClient
import io.vyne.utils.log
import lang.taxi.annotations.DataType
import lang.taxi.annotations.Service
import lang.taxi.generators.java.DefaultServiceMapper
import lang.taxi.generators.java.ServiceMapper
import lang.taxi.generators.java.TaxiGenerator
import lang.taxi.generators.java.extensions.ServiceDiscoveryAddressProvider
import lang.taxi.generators.java.extensions.SpringMvcHttpOperationExtension
import lang.taxi.generators.java.extensions.SpringMvcHttpServiceExtension
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration.DOCKER_NETWORK_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_LABELS
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryConfiguration.DOCKER_SERVICE_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.DockerSwarmDiscoveryStrategyFactory
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_DOCKER_NETWORK_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_DOCKER_SERVICE_LABELS
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_DOCKER_SERVICE_NAMES
import org.bitsofinfo.hazelcast.discovery.docker.swarm.SwarmAddressPicker.PROP_HAZELCAST_PEER_PORT
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.Optional


@Configuration
@AutoConfigureAfter(VyneConfigRegistrar::class, RibbonAutoConfiguration::class)
// TODO : re-enbale external schema services.
// https://gitlab.com/vyne/vyne/issues/14
//@EnableFeignClients(basePackageClasses = arrayOf(SchemaService::class))
// Don't enable Vyne if we're configuring to be a Schema Discovery service
@ConditionalOnMissingClass("io.vyne.schemaStore.TaxiSchemaService")

// If someone is only running a VyneClient,(ie @EnableVyneClient) they don't want the stuff inside this config
// If they've @EnableVynePublisher, then a LocalTaxiSchemaProvider will have been configured.
@ConditionalOnBean(LocalTaxiSchemaProvider::class)
class VyneAutoConfiguration {

//   @Bean
//   fun schemaStoreClient(schemaService: SchemaService): SchemaStoreClient {
//      return SchemaStoreClient(schemaService)
//   }

   @Bean
   fun vyneFactory(schemaProvider: SchemaSourceProvider, operationInvokers: List<OperationInvoker>): VyneFactory {
      return VyneFactory(schemaProvider, operationInvokers)
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
                                    serviceUrlResolvers: List<ServiceUrlResolver>,
                                    @Value("\${vyne.data-lineage.remoteCalls.enabled:false}") enableDataLineageForRemoteCalls: Boolean
   ): RestTemplateInvoker {
      return RestTemplateInvoker(schemaProvider, restTemplateBuilder, serviceUrlResolvers, enableDataLineageForRemoteCalls)
   }

   @Bean
   fun serviceDiscoveryUrlResolver(discoveryClient: DiscoveryClient): ServiceDiscoveryClientUrlResolver {
      return ServiceDiscoveryClientUrlResolver(SpringServiceDiscoveryClient(discoveryClient))
   }

   @Bean
   fun absoluteUrlResolver(): AbsoluteUrlResolver {
      return AbsoluteUrlResolver()
   }

   @Bean
   @Primary
   fun schemaProvider(localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                      remoteTaxiSchemaProvider: Optional<RemoteTaxiSourceProvider>): SchemaSourceProvider {
      return if (remoteTaxiSchemaProvider.isPresent) remoteTaxiSchemaProvider.get() else localTaxiSchemaProvider
   }

   @Bean("hazelcast")
   @ConditionalOnProperty("vyne.publicationMethod", havingValue = "DISTRIBUTED")
   @Profile("!swarm")
   fun defaultHazelCastInstance(): HazelcastInstance {
      return Hazelcast.newHazelcastInstance()
   }

   @Bean("hazelcast")
   @ConditionalOnProperty("vyne.publicationMethod", havingValue = "DISTRIBUTED")
   @Profile("swarm")
   fun swarmHazelCastInstance(): HazelcastInstance {
      val dockerNetworkName = System.getenv("DOCKER_NETWORK_NAME") ?: System.getProperty(PROP_DOCKER_NETWORK_NAMES)
      val dockerServiceName = System.getenv("DOCKER_SERVICE_NAME") ?: System.getProperty(PROP_DOCKER_SERVICE_NAMES)
      val dockerServiceLabel = System.getenv("DOCKER_SERVICE_LABELS") ?: System.getProperty(PROP_DOCKER_SERVICE_LABELS)
      val hazelcastPeerPortString  = System.getenv("HAZELCAST_PEER_PORT") ?: System.getProperty(PROP_HAZELCAST_PEER_PORT)
      val hazelcastPeerPort = hazelcastPeerPortString?.let { it.toInt() } ?: 5701
      val swarmedConfig = Config().apply {
         networkConfig.join.multicastConfig.isEnabled = false
         networkConfig.memberAddressProviderConfig.isEnabled = true
         networkConfig.join.discoveryConfig.addDiscoveryStrategyConfig(
            DiscoveryStrategyConfig(DockerSwarmDiscoveryStrategyFactory(), mapOf(
               DOCKER_NETWORK_NAMES.key() to dockerNetworkName,
               DOCKER_SERVICE_NAMES.key() to dockerServiceName,
               DOCKER_SERVICE_LABELS.key() to dockerServiceLabel
            ).filterValues { it != null })
         )
      }
      HazelcastInstanceFactory.newHazelcastInstance(swarmedConfig, null, object: DefaultNodeContext() {
         override fun createAddressPicker(node: Node): AddressPicker {
            return SwarmAddressPicker(Slf4jFactory().getLogger("SwarmAddressPicker"), dockerNetworkName, dockerServiceName, dockerServiceLabel, hazelcastPeerPort)
         }
      })
      return Hazelcast.newHazelcastInstance(swarmedConfig)
   }
}

class VyneConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }


   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name)
      val basePackageClasses = attributes["basePackageClasses"] as Array<Class<*>>
      val schemaFileInClassPath = attributes["schemaFile"] as String

      val basePackages = basePackageClasses.map { it.`package`.name } + Class.forName(importingClassMetadata.className).`package`.name

      val serviceMapper = serviceMapper(environment!!)
      val taxiGenerator = TaxiGenerator(serviceMapper = serviceMapper)

      if (schemaFileInClassPath.isBlank()) {
         registry.registerBeanDefinition("localTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(LocalTaxiSchemaProvider::class.java)
            .addConstructorArgValue(scanForCandidates(basePackages, DataType::class.java))
            .addConstructorArgValue(scanForCandidates(basePackages, Service::class.java))
            .addConstructorArgValue(taxiGenerator)
            .addConstructorArgValue(null)
            .beanDefinition)
      } else {
         registry.registerBeanDefinition("localTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(LocalTaxiSchemaProvider::class.java)
            .addConstructorArgValue(scanForCandidates(basePackages, DataType::class.java))
            .addConstructorArgValue(scanForCandidates(basePackages, Service::class.java))
            .addConstructorArgValue(taxiGenerator)
            .addConstructorArgValue(ClassPathSchemaSourceProvider(schemaFileInClassPath))
            .beanDefinition)
      }


      when (attributes["publicationMethod"] as SchemaPublicationMethod) {
//         SchemaPublicationMethod.NONE -> log().info("Not using a remote schema store")
         SchemaPublicationMethod.REMOTE -> configureHttpSchemaStore(registry)
         SchemaPublicationMethod.DISTRIBUTED -> configureHazelcastSchemaStore(registry)
      }

      if (environment!!.containsProperty("vyne.schema.name")) {
         registry.registerBeanDefinition("LocalSchemaPublisher", BeanDefinitionBuilder.genericBeanDefinition(LocalSchemaPublisher::class.java)
            .addConstructorArgValue(environment!!.getProperty("vyne.schema.name"))
            .addConstructorArgValue(environment!!.getProperty("vyne.schema.version"))
            .beanDefinition
         )
      } else {
         log().warn("Vyne is enabled, but no schema name is defined.  This application is not publishing any schemas.  If it should be, define vyne.schema.name & vyne.schema.version")
      }
   }

   private fun registerRemoteSchemaProvider(registry: BeanDefinitionRegistry, schemaStoreClientBeanName: String) {
      log().debug("Enabling remote schema store")
      registry.registerBeanDefinition("RemoteTaxiSchemaProvider", BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
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
      environment!!.propertySources.addLast(MapPropertySource("VyneHazelcastProperties", mapOf("vyne.publicationMethod" to SchemaPublicationMethod.DISTRIBUTED.name)))
   }

   private fun configureHttpSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using an Http based schema store")
      val schemaStoreClientBeanName = registry.registerBeanDefinitionOfType(HttpSchemaStoreClient::class.java)
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)
   }

   private fun serviceMapper(env: Environment): ServiceMapper {
      val applicationName = env.getProperty("spring.application.name")
         ?: error("Currently, only service-discovery enabled services are supported.  Please define spring.application.name in properties")
      val operationExtensions = listOf(SpringMvcHttpOperationExtension())
      val serviceExtensions = listOf(SpringMvcHttpServiceExtension(
         ServiceDiscoveryAddressProvider(applicationName)))
      return DefaultServiceMapper(operationExtensions = operationExtensions, serviceExtensions = serviceExtensions)
   }

   private fun scanForCandidates(basePackages: List<String>, annotationClass: Class<out Annotation>): List<Class<*>> {
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
