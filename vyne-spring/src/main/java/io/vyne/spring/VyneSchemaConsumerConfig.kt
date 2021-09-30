package io.vyne.spring

import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment

/**
 * Config which wires up various schema consumers, based on the defined method.
 * Most of the config is deferred to the SchemaStoreConfigRegistrar, which is common
 * between SchemaConsumer and SchemaPublisher
 */
class VyneSchemaConsumerConfigRegistrar : ImportBeanDefinitionRegistrar, EnvironmentAware {
   private var environment: ConfigurableEnvironment? = null
   override fun setEnvironment(environment: Environment?) {
      this.environment = environment as ConfigurableEnvironment
   }


}
