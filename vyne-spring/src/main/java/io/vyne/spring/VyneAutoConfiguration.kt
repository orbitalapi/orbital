package io.vyne.spring

import com.hazelcast.config.Config
import com.hazelcast.config.ExecutorConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.eureka.one.EurekaOneDiscoveryStrategyFactory
import com.netflix.discovery.EurekaClient
import io.vyne.schemaStore.HazelcastSchemaStoreClient
import io.vyne.schemaStore.HttpSchemaStoreClient
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.TaxiSchemaStoreService
import io.vyne.schemaStore.TaxiSchemaValidator
import io.vyne.schemaStore.eureka.EurekaClientSchemaMetaPublisher
import io.vyne.spring.config.HazelcastDiscovery
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import io.vyne.spring.projection.VyneHazelcastMemberTags
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.context.annotation.Primary
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.util.*


const val VYNE_SCHEMA_PUBLICATION_METHOD = "vyne.schema.publicationMethod"
const val VYNE_PROJECTION_DISTRIBUTIONMODE = "vyne.projection.distributionMode"

val logger = KotlinLogging.logger {}

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
class VyneAutoConfiguration(val vyneHazelcastConfiguration: VyneSpringHazelcastConfiguration, val eurekaClient: EurekaClient?) {

   @Bean
   @Primary
   fun schemaProvider(localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                      remoteTaxiSchemaProvider: Optional<RemoteTaxiSourceProvider>): SchemaSourceProvider {
      return if (remoteTaxiSchemaProvider.isPresent) remoteTaxiSchemaProvider.get() else localTaxiSchemaProvider
   }

   @Bean("hazelcast")
   @ConditionalOnExpression("'\${vyne.schema.publicationMethod}' == 'DISTRIBUTED' ||  '\${vyne.projection.distributionMode}' == 'DISTRIBUTED'")
   fun vyneHazelcastInstance(): HazelcastInstance {

      val hazelcastConfiguration = Config()
      hazelcastConfiguration.executorConfigs["projectionExecutorService"] = projectionExecutorServiceConfig()

      EurekaOneDiscoveryStrategyFactory.setEurekaClient(eurekaClient)

      when (vyneHazelcastConfiguration.discovery) {
         HazelcastDiscovery.MULTICAST -> hazelcastConfiguration.apply { multicastHazelcastConfig(this) }
         HazelcastDiscovery.AWS -> hazelcastConfiguration.apply { awsHazelcastConfig(this) }
         HazelcastDiscovery.EUREKA -> {
            hazelcastConfiguration.apply { eurekaHazelcastConfig(this, vyneHazelcastConfiguration.eurekaUri) }
         }
      }

      val instance = Hazelcast.newHazelcastInstance(hazelcastConfiguration)
      instance.cluster.localMember.setStringAttribute(VyneHazelcastMemberTags.VYNE_TAG.tag, VyneHazelcastMemberTags.QUERY_SERVICE_TAG.tag)

      return instance

   }

   fun awsHazelcastConfig(config:Config): Config {

      val AWS_REGION = System.getenv("AWS_REGION") ?: System.getProperty("AWS_REGION")

      config.executorConfigs["projectionExecutorService"] = projectionExecutorServiceConfig()
      config.networkConfig.join.multicastConfig.isEnabled = false
      config.networkConfig.join.awsConfig.isEnabled = true
      config.networkConfig.join.awsConfig.region = AWS_REGION
      config.networkConfig.join.awsConfig
         .setProperty("hz-port", vyneHazelcastConfiguration.awsPortScanRange)
         .setProperty("region", AWS_REGION)

      if (vyneHazelcastConfiguration.networkInterface.isNotEmpty()) {
         config.setProperty("hazelcast.socket.bind.any", "false")
         config.networkConfig.interfaces.isEnabled = true
         config.networkConfig.interfaces.interfaces = listOf(vyneHazelcastConfiguration.networkInterface)
      }

      return config
   }

   fun multicastHazelcastConfig(config:Config): Config {
      config.networkConfig.join.multicastConfig.isEnabled = true
      if (vyneHazelcastConfiguration.networkInterface.isNotEmpty()) {
         config.setProperty("hazelcast.socket.bind.any", "false")
         config.networkConfig.interfaces.isEnabled = true
         config.networkConfig.interfaces.interfaces = listOf(vyneHazelcastConfiguration.networkInterface)
      }
      return config
   }

   fun eurekaHazelcastConfig(config:Config, eurekaUri: String): Config {

      config.apply {

          networkConfig.join.tcpIpConfig.isEnabled = false
          networkConfig.join.multicastConfig.isEnabled = false
          networkConfig.join.eurekaConfig.isEnabled = true
          networkConfig.join.eurekaConfig.setProperty("self-registration", "true")
          networkConfig.join.eurekaConfig.setProperty("namespace", "hazelcast")
          networkConfig.join.eurekaConfig.setProperty("use-metadata-for-host-and-port", vyneHazelcastConfiguration.useMetadataForHostAndPort)
          networkConfig.join.eurekaConfig.setProperty("use-classpath-eureka-client-props", "false")
          networkConfig.join.eurekaConfig.setProperty("shouldUseDns", "false")
          networkConfig.join.eurekaConfig.setProperty("serviceUrl.default", eurekaUri)

          if (vyneHazelcastConfiguration.networkInterface.isNotEmpty()) {
             config.setProperty("hazelcast.socket.bind.any", "false")
             networkConfig.interfaces.isEnabled = true
             networkConfig.interfaces.interfaces = listOf(vyneHazelcastConfiguration.networkInterface)
          }
      }

      return config

   }

   fun projectionExecutorServiceConfig():ExecutorConfig {

      val projectionExecutorServiceConfig = ExecutorConfig()
      projectionExecutorServiceConfig.poolSize = vyneHazelcastConfiguration.taskPoolSize
      projectionExecutorServiceConfig.queueCapacity = vyneHazelcastConfiguration.taskQueueSize
      projectionExecutorServiceConfig.isStatisticsEnabled = true
      return projectionExecutorServiceConfig
   }
}

class VyneConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }


   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
      val attributes = importingClassMetadata.getAnnotationAttributes(VyneSchemaPublisher::class.java.name)
      val isVyneQueryServer = importingClassMetadata.isAnnotated(VyneQueryServer::class.java.name)
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

      val annotationPublicationMethod = attributes["publicationMethod"] as SchemaPublicationMethod
      val publicationMethod = environment!!.getProperty(VYNE_SCHEMA_PUBLICATION_METHOD, annotationPublicationMethod.name)

      log().info("${VYNE_SCHEMA_PUBLICATION_METHOD}=${publicationMethod}")

      when (SchemaPublicationMethod.valueOf(publicationMethod)) {
         //SchemaPublicationMethod.DISABLED -> log().info("Not using a remote schema store")
         SchemaPublicationMethod.LOCAL -> configureLocalSchemaStore(registry)
         SchemaPublicationMethod.REMOTE -> configureHttpSchemaStore(registry)
         SchemaPublicationMethod.EUREKA -> configureEurekaSchemaStore(registry, isVyneQueryServer)
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

   private fun configureEurekaSchemaStore(registry: BeanDefinitionRegistry, isVyneQueryServer: Boolean) {
      log().debug("Enabling Eureka based schema store")
      if (!isVyneQueryServer) {
         registry.registerBeanDefinition(EurekaClientSchemaMetaPublisher::class.simpleName!!,
            BeanDefinitionBuilder.genericBeanDefinition(EurekaClientSchemaMetaPublisher::class.java)
               .beanDefinition)
      }

      if (isVyneQueryServer) {
         registry.registerBeanDefinition("RemoteTaxiSchemaProvider",
            BeanDefinitionBuilder.genericBeanDefinition(RemoteTaxiSourceProvider::class.java)
               .addConstructorArgReference("eurekaClientConsumer")
               .beanDefinition
         )
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

      environment !!.propertySources.addLast(MapPropertySource("VyneHazelcastProperties", mapOf(VYNE_SCHEMA_PUBLICATION_METHOD to SchemaPublicationMethod.DISTRIBUTED.name)))
   }

   private fun configureLocalSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using local schema store")
      val schemaStoreClientBeanName = registry.registerBeanDefinitionOfType(LocalValidatingSchemaStoreClient::class.java)
      registry.registerBeanDefinitionOfType(TaxiSchemaStoreService::class.java)
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)
   }

   private fun configureHttpSchemaStore(registry: BeanDefinitionRegistry) {
      log().info("Using an Http based schema store")
      registry.registerBeanDefinitionOfType(HttpSchemaStoreFeignConfig::class.java)
      val schemaStoreClientBeanName = registry.registerBeanDefinitionOfType(HttpSchemaStoreClient::class.java)
      registerRemoteSchemaProvider(registry, schemaStoreClientBeanName)
   }

   private fun serviceMapper(env: Environment): ServiceMapper {
      val applicationName = env.getProperty("spring.application.name")
         ?: error("Currently, only service-discovery enabled services are supported.  Please define spring.application.name in properties")

      val contextPath = env.getProperty("server.servlet.context-path")?:""
      val operationExtensions = listOf(SpringMvcHttpOperationExtension(contextPath))
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
   this.registerBeanDefinition(
      beanName,
      BeanDefinitionBuilder.genericBeanDefinition(clazz).beanDefinition
   )
   return beanName
}
