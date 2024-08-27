package com.orbitalhq.plugins

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * Simple wrapper to make a PluginLoader that was initialized outside of spring available inside the spring context
 */
class PluginLoaderInitializer(private val pluginLoader: PluginLoader): ApplicationContextInitializer<ConfigurableApplicationContext> {
   override fun initialize(applicationContext: ConfigurableApplicationContext) {
      applicationContext.beanFactory.registerSingleton("pluginLoader", pluginLoader)
   }
}
