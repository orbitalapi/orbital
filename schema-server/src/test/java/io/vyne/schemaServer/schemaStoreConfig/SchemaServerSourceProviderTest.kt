package io.vyne.schemaServer.schemaStoreConfig

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import java.time.Duration

class SchemaServerSourceProviderTest {

   @Test
   fun `can register schema and list summary`() {
      val schemaSourceProvider = SchemaServerSourceProvider(
         emptyList(),
         jacksonObjectMapper()
      )
      val personSchema = TaxiSchema.from(
         """
         type FirstName inherits String
         type LastName inherits String
         model Person {
            firstName : FirstName
            lastName : LastName
         }
      """.trimIndent()
      )
      val filmSchema = TaxiSchema.from(
         """
         type FilmName inherits String
      """.trimIndent()
      )
      schemaSourceProvider.submitSources(
         VersionedSourceSubmission(
            personSchema.sources, PublisherConfiguration(
               "person-schema-id"
            )
         )
      ).block()
      schemaSourceProvider.submitSources(
         VersionedSourceSubmission(
            filmSchema.sources, PublisherConfiguration(
               "film-schema-id"
            )
         )
      ).block(Duration.ofSeconds(10))

      val summaries = schemaSourceProvider.listSchemaSummaries().block()!!
      summaries.single { it.publisherId == "person-schema-id" }.typeNames.should.have.size(3)
      summaries.single { it.publisherId == "film-schema-id" }.typeNames.should.have.size(1)
   }
}
