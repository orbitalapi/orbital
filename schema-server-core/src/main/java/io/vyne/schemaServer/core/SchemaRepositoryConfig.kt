package io.vyne.schemaServer.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.registerCustomType
import io.github.config4k.toConfig
import io.vyne.config.BaseHoconConfigFileRepository
import io.vyne.config.toConfig
import io.vyne.schemaServer.core.adaptors.InstantHoconSupport
import io.vyne.schemaServer.core.adaptors.PackageLoaderSpecHoconSupport
import io.vyne.schemaServer.core.adaptors.UriHoconSupport
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.utils.concat
import java.nio.file.Path

data class SchemaRepositoryConfig(
   val file: FileSystemSchemaRepositoryConfig? = null,
   val openApi: OpenApiSchemaRepositoryConfig? = null,
   val git: GitSchemaRepositoryConfig? = null
)


class InMemorySchemaRepositoryConfigLoader(private var config: SchemaRepositoryConfig) : SchemaRepositoryConfigLoader {
   override fun load(): SchemaRepositoryConfig = config
   override fun safeConfigJson(): String {
      return jacksonObjectMapper().writerWithDefaultPrettyPrinter()
         .writeValueAsString(config)
   }

   override fun addFileSpec(fileSpec: FileSystemPackageSpec) {
      config = config.copy(
         file = config.file!!.copy(
            projects = config.file!!.projects.concat(fileSpec)
         )
      )
   }

   override fun addGitSpec(gitSpec: GitRepositoryConfig) {
      TODO("Not yet implemented")
   }
}

interface SchemaRepositoryConfigLoader {
   fun load(): SchemaRepositoryConfig
   fun safeConfigJson(): String
   fun addFileSpec(fileSpec: FileSystemPackageSpec)

   fun addGitSpec(gitSpec: GitRepositoryConfig)
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

   private fun makeRelativeToConfigFile(path: Path): Path {
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

   override fun addFileSpec(fileSpec: FileSystemPackageSpec) {
      val current = this.typedConfig() // Don't call load, as we want the original, not the one we resolve paths with
      val currentFileConfig = current.file ?: FileSystemSchemaRepositoryConfig()

      if (currentFileConfig.projects.any {
            it.path == fileSpec.path
         }) {
         throw BadRepositorySpecException("${fileSpec.path} already exists")
      }

      val updated = current.copy(
         file = currentFileConfig.copy(
            projects = currentFileConfig.projects.concat(fileSpec)
         )
      )
      save(updated)
   }

   override fun addGitSpec(gitSpec: GitRepositoryConfig) {
      TODO("Not yet implemented")
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

class BadRepositorySpecException(message: String) : RuntimeException(message)
