package io.vyne.schemaServer.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import java.nio.file.Path

data class SchemaRepositoryConfig(
   val file: FileSystemSchemaRepositoryConfig? = null,
   val openApi: OpenApiSchemaRepositoryConfig? = null,
   val git: GitSchemaRepositoryConfig? = null
)


class InMemorySchemaRepositoryConfigLoader(val config: io.vyne.schemaServer.core.SchemaRepositoryConfig) : io.vyne.schemaServer.core.SchemaRepositoryConfigLoader {
   override fun load(): io.vyne.schemaServer.core.SchemaRepositoryConfig = config
   override fun safeConfigJson(): String {
      return jacksonObjectMapper().writerWithDefaultPrettyPrinter()
         .writeValueAsString(config)
   }
}

interface SchemaRepositoryConfigLoader {
   fun load(): io.vyne.schemaServer.core.SchemaRepositoryConfig
   fun safeConfigJson(): String
}

class FileSchemaRepositoryConfigLoader(path: Path, fallback: Config = ConfigFactory.systemProperties()) :
   BaseHoconConfigFileRepository<io.vyne.schemaServer.core.SchemaRepositoryConfig>(
      path, fallback
   ), io.vyne.schemaServer.core.SchemaRepositoryConfigLoader {
   override fun extract(config: Config): io.vyne.schemaServer.core.SchemaRepositoryConfig = config.extract()

   override fun emptyConfig(): io.vyne.schemaServer.core.SchemaRepositoryConfig = io.vyne.schemaServer.core.SchemaRepositoryConfig(null, null, null)

   override fun safeConfigJson(): String {
      return getSafeConfigString(unresolvedConfig(), asJson = true)
   }

   override fun load(): io.vyne.schemaServer.core.SchemaRepositoryConfig {
      return typedConfig()
   }

   fun save(schemaRepoConfig: io.vyne.schemaServer.core.SchemaRepositoryConfig) {
      val newConfig = schemaRepoConfig.toConfig()

      // Use the existing unresolvedConfig to ensure that when we're
      // writing back out, that tokens that have been resolved
      // aren't accidentally written with their real values back out
      val existingValues = unresolvedConfig()

      val updated = ConfigFactory.empty()
         .withFallback(newConfig)
         .withFallback(existingValues)

      saveConfig(updated)
   }
}
