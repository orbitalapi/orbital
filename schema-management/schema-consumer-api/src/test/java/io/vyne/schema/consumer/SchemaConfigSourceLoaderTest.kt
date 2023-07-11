package io.vyne.schema.consumer

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.api.SchemaSet
import io.vyne.schemas.taxi.TaxiSchema
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
            "@orbital/config" to listOf(
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
            "@orbital/config" to listOf(
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
}
