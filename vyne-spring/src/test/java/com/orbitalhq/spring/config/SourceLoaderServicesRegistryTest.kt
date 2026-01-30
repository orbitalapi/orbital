package com.orbitalhq.spring.config

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigFileLocationConventions
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaConfigSourceLoader
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import lang.taxi.packages.SourcesType

class SourceLoaderServicesRegistryTest : DescribeSpec({
   describe("loading services.conf") {
      it("will merge env specific config overriding base config") {
         val schemaStore = schemaStoreWithSources(
            mapOf(
               ConfigFileLocationConventions.OrbitalConfigKey to listOf(
                  VersionedSource("/config/auth.conf", "1.0.0", $$"""
services {
   "productsApi" {
      url="http://localhost:"${NEBULA_HTTP_PORT}
   }
}
""".trimMargin()),
                  VersionedSource("/config/auth.prod.conf", "1.0.0", $$"""
services {
   "productsApi" {
      url="http://localhost:1234"
   }
}
                  """.trimMargin()),
               )
            )
         )
         val loader = SchemaConfigSourceLoader(schemaStore, "auth.conf", environmentName = "prod")
         val servicesConfig = SourceLoaderServicesRegistry(
            loaders = listOf(loader)
         ).typedConfig()
         servicesConfig.services["productsApi"]!!.get("url").shouldBe("http://localhost:1234")
      }
   }

})


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
