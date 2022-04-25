package io.vyne.schema.spring.config.consumer

import io.vyne.schema.consumer.http.HttpSchemaStore
import io.vyne.schema.consumer.rsocket.RSocketSchemaStore
import io.vyne.schema.spring.config.RSocketTransportConfig
import io.vyne.schema.spring.config.SchemaConfigProperties
import io.vyne.schema.spring.config.consumer.SchemaConsumerConfigProperties.Companion.CONSUMER_METHOD
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableConfigurationProperties(
   SchemaConsumerConfigProperties::class,
   SchemaConfigProperties::class
)
class SchemaConsumerConfig

@ConditionalOnProperty(CONSUMER_METHOD, havingValue = "Http", matchIfMissing = false)
@Configuration
@Import(HttpSchemaStore::class)
class VyneHttpSchemaStoreConfig

@ConditionalOnProperty(CONSUMER_METHOD, havingValue = "RSocket", matchIfMissing = true)
@Configuration
@Import(RSocketSchemaStore::class, RSocketTransportConfig::class)
class VyneRSocketSchemaStoreConfig {

}


class VyneConsumerRegistrar /*: ImportBeanDefinitionRegistrar, EnvironmentAware */ {
//   private lateinit var environment: ConfigurableEnvironment
//   override fun setEnvironment(env: Environment) {
//      this.environment = env as ConfigurableEnvironment
//   }
//
//   override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
//      val consumptionMethodString: String? = environment.getProperty(VYNE_SCHEMA_CONSUMPTION_METHOD)
//      consumptionMethodString?.let {
//         when (VyneSchemaInteractionMethod.tryParse(it)) {
//            VyneSchemaInteractionMethod.RSOCKET -> {
//               registerRSocketProxy(environment, registry)
//               registry.registerBeanDefinition(
//                  "rSocketSchemaStore",
//                  BeanDefinitionBuilder
//                     .genericBeanDefinition(RSocketSchemaStore::class.java)
//                     .beanDefinition
//               )
//            }
//
//            VyneSchemaInteractionMethod.HTTP -> {
//               registerHttpConsumer(registry)
//            }
//         }
//      }
//
//   }
//
//   fun registerHttpConsumer(registry: BeanDefinitionRegistry) {
//      // Enable relevant feign client.
//      val httpSchemaStoreFeignConfigClass = HttpSchemaListSchemasFeignConfig::class.java
//      registry.registerBeanDefinition(
//         httpSchemaStoreFeignConfigClass.simpleName,
//         BeanDefinitionBuilder.genericBeanDefinition(httpSchemaStoreFeignConfigClass).beanDefinition
//      )
//
//      // Register Http based Schema Store
//      registry.registerBeanDefinition(
//         "httpSchemaStore",
//         BeanDefinitionBuilder.genericBeanDefinition(HttpSchemaStore::class.java)
//            .beanDefinition
//      )
//   }

   companion object {
//      fun registerRSocketProxy(environment: ConfigurableEnvironment, registry: BeanDefinitionRegistry) {
//         val schemaServerDiscoveryClientNamePropertyName = "vyne.schema-server.name"
//         if (!registry.containsBeanDefinition("rSocketSchemaServerProxy")) {
//            registry.registerBeanDefinition(
//               "rSocketSchemaServerProxy",
//               BeanDefinitionBuilder
//                  .genericBeanDefinition(RSocketSchemaServerProxy::class.java)
//                  .addConstructorArgValue(
//                     environment.getProperty(
//                        schemaServerDiscoveryClientNamePropertyName,
//                        "schema-server"
//                     )
//                  )
//                  .addConstructorArgReference("discoveryClient")
//                  .beanDefinition
//            )
//         }
//      }
//   }
   }
}
