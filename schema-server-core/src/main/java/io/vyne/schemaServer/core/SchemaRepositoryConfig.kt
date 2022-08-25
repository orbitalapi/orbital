package io.vyne.schemaServer.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.registerCustomType
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import io.vyne.schemaServer.core.adaptors.InstantHoconSupport
import io.vyne.schemaServer.core.adaptors.PackageLoaderSpecHoconSupport
import io.vyne.schemaServer.core.adaptors.UriHoconSupport
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import java.nio.file.Path

data class SchemaRepositoryConfig(
   val file: FileSystemSchemaRepositoryConfig? = null,
   val openApi: OpenApiSchemaRepositoryConfig? = null,
   val git: GitSchemaRepositoryConfig? = null
)


class InMemorySchemaRepositoryConfigLoader(val config: SchemaRepositoryConfig) : SchemaRepositoryConfigLoader {
   override fun load(): SchemaRepositoryConfig = config
   override fun safeConfigJson(): String {
      return jacksonObjectMapper().writerWithDefaultPrettyPrinter()
         .writeValueAsString(config)
   }
}

interface SchemaRepositoryConfigLoader {
   fun load(): SchemaRepositoryConfig
   fun safeConfigJson(): String
}

class FileSchemaRepositoryConfigLoader(
   private val configFilePath: Path,
   fallback: Config = ConfigFactory.systemEnvironment()
) :
   BaseHoconConfigFileRepository<SchemaRepositoryConfig>(
      configFilePath, fallback
   ), SchemaRepositoryConfigLoader {
   init {
       registerCustomType(PackageLoaderSpecHoconSupport)
       registerCustomType(UriHoconSupport)
       registerCustomType(InstantHoconSupport)
   }

   override fun extract(config: Config): SchemaRepositoryConfig = config.extract()

   override fun emptyConfig(): SchemaRepositoryConfig = SchemaRepositoryConfig(null, null, null)

   override fun safeConfigJson(): String {
      return getSafeConfigString(unresolvedConfig(), asJson = true)
   }

   override fun load(): SchemaRepositoryConfig {
      val original = typedConfig()
      return resolveRelativePaths(original)
   }

   private fun makeRelativeToConfigFile(path:Path):Path {
      return if (path.isAbsolute) {
         path
      } else {
         configFilePath.parent.resolve(path)
      }
   }

   private fun resolveRelativePaths(original: SchemaRepositoryConfig): SchemaRepositoryConfig {
      val updatedFileConfig = original.file?.let { fileConfig ->
         val resolvedPaths = fileConfig.paths.map { makeRelativeToConfigFile(it) }
         val apiEditorPath = fileConfig.apiEditorProjectPath?.let { makeRelativeToConfigFile(it) }
         fileConfig.copy(paths = resolvedPaths, apiEditorProjectPath = apiEditorPath)
      }
      return original.copy(file = updatedFileConfig)
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
