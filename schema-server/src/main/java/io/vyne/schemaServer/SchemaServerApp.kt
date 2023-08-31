package io.vyne.schemaServer

import io.vyne.connectors.soap.SoapWsdlSourceConverter
import io.vyne.monitoring.EnableCloudMetrics
import io.vyne.schemaServer.core.VersionedSourceLoader
import io.vyne.schemaServer.core.config.WorkspaceConfig
import io.vyne.schemas.readers.SourceConverterRegistry
import io.vyne.schemas.readers.TaxiSourceConverter
import io.vyne.spring.config.VyneSpringHazelcastConfiguration
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = KotlinLogging.logger {}

@EnableAsync
@SpringBootApplication(scanBasePackageClasses = [SchemaServerApp::class, VersionedSourceLoader::class])
@EnableScheduling
// MP: I don't think Schema server needs a discovery client, does it?
//@EnableDiscoveryClient
@EnableConfigurationProperties(
   value = [
      VyneSpringHazelcastConfiguration::class,
      WorkspaceConfig::class
   ]
)
class SchemaServerApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         SpringApplication.run(SchemaServerApp::class.java, *args)
      }
   }

   @Bean
   fun sourceConverterRegistry(): SourceConverterRegistry = SourceConverterRegistry(
      setOf(
         TaxiSourceConverter,
         SoapWsdlSourceConverter
      ),
      registerWithStaticRegistry = true
   )


   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val baseVersion = buildInfo?.get("baseVersion")
      val buildNumber = buildInfo?.get("buildNumber")
      val version = if (!baseVersion.isNullOrEmpty() && buildNumber != "0" && buildInfo.version.contains("SNAPSHOT")) {
         "$baseVersion-BETA-$buildNumber"
      } else {
         buildInfo?.version ?: "Dev version"
      }

      logger.info { "Schema server version => $version" }
   }
}


@EnableCloudMetrics
class SchemaServerConfig

