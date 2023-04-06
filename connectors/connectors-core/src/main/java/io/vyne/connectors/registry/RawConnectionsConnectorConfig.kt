package io.vyne.connectors.registry

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import io.vyne.connectors.VyneConnectionsConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * Provides the full connections.conf,
 * with env resolutions applied.
 *
 * Used so we can ship the connections off to a standalone
 * query node.
 */
@Component
class RawConnectionsConnectorConfig(
   private val path: Path,
   private val fallback: Config = ConfigFactory.systemEnvironment(),
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
   @Autowired
   constructor(connectionConfig: VyneConnectionsConfig) : this(connectionConfig.configFile)

   fun loadAsMap(): Map<String, Any> {
      val configFileContent = path.toFile().readText(Charset.defaultCharset())
      val substitutedRawConfig = ConfigFactory
         .parseString(configFileContent, ConfigParseOptions.defaults())
         .resolveWith(fallback, ConfigResolveOptions.defaults().setAllowUnresolved(true))
      return substitutedRawConfig.root().unwrapped()
   }
}
