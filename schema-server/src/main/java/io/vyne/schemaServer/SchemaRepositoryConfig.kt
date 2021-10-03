package io.vyne.schemaServer

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import io.vyne.schemaServer.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.openapi.OpenApiSchemaRepositoryConfig
import java.nio.file.Path

data class SchemaRepositoryConfig(
   val file: FileSystemSchemaRepositoryConfig? = null,
   val openApi: OpenApiSchemaRepositoryConfig? = null,
   val git: GitSchemaRepositoryConfig? = null
)


class InMemorySchemaRepositoryConfigLoader(val config:SchemaRepositoryConfig): SchemaRepositoryConfigLoader {
   override fun load(): SchemaRepositoryConfig = config
}
interface SchemaRepositoryConfigLoader {
   fun load(): SchemaRepositoryConfig
}

class FileSchemaRepositoryConfigLoader(path: Path, fallback: Config = ConfigFactory.systemProperties()) :
   BaseHoconConfigFileRepository<SchemaRepositoryConfig>(
      path, fallback
   ), SchemaRepositoryConfigLoader {
   override fun extract(config: Config): SchemaRepositoryConfig = config.extract()

   override fun emptyConfig(): SchemaRepositoryConfig = SchemaRepositoryConfig(null, null, null)

   fun safeConfigJson(): String {
      return getSafeConfigString(unresolvedConfig(), asJson = true)
   }

   override fun load(): SchemaRepositoryConfig {
      return typedConfig()
   }

   fun save(schemaRepoConfig: SchemaRepositoryConfig) {
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