package io.vyne.schemaServer.packages

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemas.PartialSchema
import org.junit.Test

class PackageServiceTest {

   @Test
   fun `can serde results`() {
      val schemaStore = LocalValidatingSchemaStoreClient()
      val packageMetadata = PackageMetadata.from("com.vyne", "movies", "0.1.0")
      schemaStore.submitPackage(
         SourcePackage(
            packageMetadata,
            listOf(
               VersionedSource.sourceOnly(
                  """
               model Movie {
                  movieTitle : Title inherits String
                  duration : Minutes inherits Int
               }
               service MovieService {
                  operation getAll():Movie[]
               }
            """.trimIndent()
               )
            )
         )
      )
      val packageService = PackageService(mock { }, schemaStore)
      val schema = packageService.getPartialSchemaForPackage(packageMetadata.identifier.uriSafeId).block()!!

      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema)
      val fromJson = jacksonObjectMapper().readValue<PartialSchema>(json)
   }

   @Test
   fun canGetServicesFromPackage() {
      val schemaStore = LocalValidatingSchemaStoreClient()
      schemaStore.submitPackage(
         SourcePackage(
            PackageMetadata.from("com.vyne", "movies", "0.1.0"),
            listOf(
               VersionedSource.sourceOnly(
                  """
               model Movie {
                  movieTitle : Title inherits String
                  duration : Minutes inherits Int
               }
               service MovieService {
                  operation getAll():Movie[]
               }
            """.trimIndent()
               )
            )
         )
      )

      val actorsPackage = PackageMetadata.from("com.vyne", "actors", "0.1.0")
      schemaStore.submitPackage(
         SourcePackage(
            actorsPackage,
            listOf(
               VersionedSource.sourceOnly(
                  """
               model Actor {
                  name : ActorName inherits String
               }
               service ActorService {
                  operation getAll():Actor[]
               }
            """.trimIndent()
               )
            )
         )
      )
      val packageService = PackageService(mock { }, schemaStore)
      val schema = packageService.getPartialSchemaForPackage(actorsPackage.identifier.uriSafeId).block()!!

      schema.services.map { it.name.name }.should.contain("ActorService")
      schema.types.map { it.name.name }.should.contain.elements("Actor", "ActorName")
      schema.types.map { it.name.name }.should.not.contain.elements("Movie", "Title")
   }
}
