package com.orbitalhq.spring.config

import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.config.ConfigSourceWriterProvider
import com.orbitalhq.config.MergingHoconConfigRepository
import com.orbitalhq.http.ServicesConfig
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

/**
 * Uses source loaders to provide services.conf loading,
 * to produce a ServicesConfig
 *
 * This allows users to define a services.conf in their project (or
 * at the server level), which is ultimately passsed to a DiscoveryClient for
 * service discovery
 */
class SourceLoaderServicesRegistry(
   private val loaders: List<ConfigSourceLoader>,
   /**
    * To enable writing, pass a writerProvider - (normally a
    * ProjectManagerConfigSourceLoader) here
    */
   writerProviders: List<ConfigSourceWriterProvider> = emptyList(),
   fallback: Config = ConfigFactory.systemEnvironment(),
): MergingHoconConfigRepository<ServicesConfig>(loaders, writerProviders, fallback) {
   override fun extract(config: Config): ServicesConfig = config.extract()
   override fun emptyConfig(): ServicesConfig = ServicesConfig()

}
