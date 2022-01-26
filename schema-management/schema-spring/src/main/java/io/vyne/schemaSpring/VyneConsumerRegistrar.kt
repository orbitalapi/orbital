package io.vyne.schemaSpring

import io.vyne.httpSchemaConsumer.HttpSchemaStore
import io.vyne.rSocketSchemaConsumer.RSocketSchemaStore
import io.vyne.schemaSpring.VyneConsumerRegistrar.Companion.VYNE_SCHEMA_CONSUMPTION_METHOD
import io.vyne.schemeRSocketCommon.RSocketSchemaServerProxy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@ConditionalOnProperty(VYNE_SCHEMA_CONSUMPTION_METHOD, havingValue = "HTTP", matchIfMissing = false)
@Configuration
@Import(HttpSchemaStore::class)
class VyneHttpSchemaStoreConfig

@ConditionalOnProperty(VYNE_SCHEMA_CONSUMPTION_METHOD, havingValue = "RSOCKET", matchIfMissing = true)
@Configuration
@Import(RSocketSchemaServerProxy::class, RSocketSchemaStore::class)
class VyneRSocketSchemaStoreConfig


class VyneConsumerRegistrar /*: ImportBeanDefinitionRegistrar, EnvironmentAware */{
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
      const val VYNE_SCHEMA_CONSUMPTION_METHOD = "vyne.schema.consumptionMethod"
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
