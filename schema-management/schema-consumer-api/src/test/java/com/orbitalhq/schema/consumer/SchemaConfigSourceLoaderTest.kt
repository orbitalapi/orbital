package com.orbitalhq.schema.consumer

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigFileLocationConventions
import com.orbitalhq.config.HoconConfigRepository
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import lang.taxi.packages.SourcesType
import org.junit.Test

class SchemaConfigSourceLoaderTest {

   private fun schemaStoreWithSources(additionalSources: Map<SourcesType, List<VersionedSource>>): SchemaStore {
      return SimpleSchemaStore(
         SchemaSet.from(
            TaxiSchema.from(
               listOf(
                  SourcePackage(
                     PackageMetadata.from(PackageIdentifier.fromId("com.foo/test/1.0.0")),
                     sources = emptyList(),
                     additionalSources = additionalSources
                  )
               )
            ),
            0
         )
      )
   }

   @Test
   fun `loads single config file`() {

      val schemaStore = schemaStoreWithSources(
         mapOf(
            ConfigFileLocationConventions.OrbitalConfigKey to listOf(
               VersionedSource("auth.conf", "1.0.0", "I am auth config"),
               VersionedSource("services.conf", "1.0.0", "I am services config"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(1)
      val loadedSource = sourcePackage.sources.single()
      loadedSource.name.shouldBe("auth.conf")
   }

   @Test
   fun `loads single config file when full paths are present`() {
      val schemaStore = schemaStoreWithSources(
         mapOf(
            ConfigFileLocationConventions.OrbitalConfigKey to listOf(
               VersionedSource("/some/path/to/auth.conf", "1.0.0", "I am auth config"),
               VersionedSource("auth.conf", "1.0.0", "I am services config"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(2)
   }


   @Test
   fun `loads multiple config files`() {
      val schemaStore = schemaStoreWithSources(
         additionalSources = mapOf(
            "@orbital/pipelines" to listOf(
               VersionedSource("pipeline-1.conf", "1.0.0", "I am pipeline 1"),
               VersionedSource("pipeline-2.conf", "1.0.0", "I am pipeline 2"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "*.conf", sourceType = "@orbital/pipelines")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(2)
   }

   @Test
   fun `loads multiple config files when full paths are present`() {
      val schemaStore = schemaStoreWithSources(
         additionalSources = mapOf(
            "@orbital/pipelines" to listOf(
               VersionedSource("/a/b/c/pipeline-1.conf", "1.0.0", "I am pipeline 1"),
               VersionedSource("/a/b/c/pipeline-2.conf", "1.0.0", "I am pipeline 2"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "*.conf", sourceType = "@orbital/pipelines")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(2)
   }

   @Test
   fun `loads environment-specific config file`() {
      val schemaStore = schemaStoreWithSources(
         mapOf(
            ConfigFileLocationConventions.OrbitalConfigKey to listOf(
               VersionedSource("auth.conf", "1.0.0", "I am base auth config"),
               VersionedSource("auth.preprod.conf", "1.0.0", "I am preprod auth config"),
               VersionedSource("services.conf", "1.0.0", "I am services config"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf", environmentName = "preprod")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(2)
      val loadedSources = sourcePackage.sources.map { it.name }.toSet()
      loadedSources.shouldBe(setOf("auth.conf", "auth.preprod.conf"))
   }

   @Test
   fun `loads environment-specific config file with wildcard pattern`() {
      val schemaStore = schemaStoreWithSources(
         additionalSources = mapOf(
            "@orbital/pipelines" to listOf(
               VersionedSource("pipeline-1.conf", "1.0.0", "I am pipeline 1"),
               VersionedSource("pipeline-1.staging.conf", "1.0.0", "I am pipeline 1 staging"),
               VersionedSource("pipeline-2.conf", "1.0.0", "I am pipeline 2"),
               VersionedSource("pipeline-2.staging.conf", "1.0.0", "I am pipeline 2 staging"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "*.conf", sourceType = "@orbital/pipelines", environmentName = "staging")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(4)
   }

   @Test
   fun `loads only base config when environment-specific file is not present`() {
      val schemaStore = schemaStoreWithSources(
         mapOf(
            ConfigFileLocationConventions.OrbitalConfigKey to listOf(
               VersionedSource("auth.conf", "1.0.0", "I am base auth config"),
               VersionedSource("services.conf", "1.0.0", "I am services config"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf", environmentName = "preprod")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(1)
      val loadedSource = sourcePackage.sources.single()
      loadedSource.name.shouldBe("auth.conf")
   }

   @Test
   fun `loads both base and environment-specific config when both are present with full paths`() {
      val schemaStore = schemaStoreWithSources(
         mapOf(
            ConfigFileLocationConventions.OrbitalConfigKey to listOf(
               VersionedSource("/config/auth.conf", "1.0.0", "I am base auth config"),
               VersionedSource("/config/auth.prod.conf", "1.0.0", "I am prod auth config"),
               VersionedSource("/config/services.conf", "1.0.0", "I am services config"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf", environmentName = "prod")
      val loaded = loader.load()
      loaded.shouldHaveSize(1)
      val sourcePackage = loaded.single()
      sourcePackage.sources.shouldHaveSize(2)
      val loadedSources = sourcePackage.sources.map { it.name }.toSet()
      loadedSources.shouldBe(setOf("/config/auth.conf", "/config/auth.prod.conf"))
   }

   @Test
   fun `can override base config with env specific config`() {
      val schemaStore = schemaStoreWithSources(
         mapOf(
            ConfigFileLocationConventions.OrbitalConfigKey to listOf(
               VersionedSource("/config/auth.conf", "1.0.0", "I am base auth config"),
               VersionedSource("/config/auth.prod.conf", "1.0.0", "I am prod auth config"),
               VersionedSource("/config/services.conf", "1.0.0", "I am services config"),
            )
         )
      )
      val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf", environmentName = "prod")
   }
}
