package com.orbitalhq.schemaServer.core.adaptors.taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemaServer.core.adaptors.avro.AvroTaxiSourceGenerator
import com.orbitalhq.schemaServer.core.adaptors.openapi.OpenApiSourceGenerator
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoader
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.files.FileSystemChangeEvent
import com.orbitalhq.utils.files.ReactiveFileSystemMonitor
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import lang.taxi.errors
import lang.taxi.packages.SourcesTypes
import org.junit.Test
import reactor.core.publisher.Sinks
import kotlin.io.path.toPath

class TaxiSchemaSourcesAdaptorTest {

   @Test
   fun `can load taxi project with avro additional sources`() {
      val source = loadSourcePackage("mixed-sources/single-avro-directory")
      val schema = TaxiSchema.from(source)
      schema.hasType("foo.AddressBook")
         .shouldBeTrue()
      schema.type("foo.addressbook.People")
         .attribute("email")
         .type.shouldBe("foo.EmailAddress".fqn())
   }
   @Test
   fun `can load taxi project with protobuf additional sources`() {
      val source = loadSourcePackage("mixed-sources/single-protobuf-directory")
      val schema = TaxiSchema.from(source)
      schema.hasType("CafeDrink")
         .shouldBeTrue()
   }
   @Test
   fun `can load taxi project with openAPI additional sources with config files`() {
      val source = loadSourcePackage("mixed-sources/openapi-with-conf-file")
      val schema = TaxiSchema.from(source)
      // types and services should be in the configured default namespace
      schema.hasType("com.mytest.petstore.NewPet")
         .shouldBeTrue()
      schema.hasService("com.mytest.petstore.PetsService")
         .shouldBeTrue()
      val findPetsOperation = schema.service("com.mytest.petstore.PetsService")
         .operation("findPets")
      val httpMetadata = findPetsOperation.metadata.single { it.name.fullyQualifiedName == "taxi.http.HttpOperation" }
      // verify the configured baseurl has been picked up
      httpMetadata.params["url"]!!.shouldBe("https://pets.com/pets")
   }


   @Test
   fun `can load taxi project with multiple Avro and OpenAPI sources`() {
      val source = loadSourcePackage("mixed-sources/avro-and-openapi")
      val schema = TaxiSchema.from(source)
      schema.compilerMessages.errors().shouldBeEmpty()
      schema.hasType("foo.AddressBook").shouldBeTrue()
      schema.hasType("music.Album").shouldBeTrue()
      schema.hasType("petstore.Pet").shouldBeTrue()
      schema.hasType("movies.Film").shouldBeTrue()


      val openApiSources = schema.additionalSources[OpenApiSourceGenerator.OPENAPI_SOURCES_TYPE]
         .shouldNotBeNull()
         .shouldHaveSize(1)
      // 2 openAPI specs
      openApiSources.single().sources.shouldHaveSize(2)

      val avroSources = schema.additionalSources[AvroTaxiSourceGenerator.AVRO_SOURCES_TYPE]
         .shouldNotBeNull()
         .shouldHaveSize(1)
      // 2 Avro specs
      avroSources.single().sources.shouldHaveSize(2)

      val originalSources = schema.additionalSources[SourcesTypes.ORIGINAL_SOURCE]
         .shouldNotBeNull()
         .shouldHaveSize(1)
      originalSources.single().sources.shouldHaveSize(4)

      val sourceMap = schema.additionalSources[SourcesTypes.SOURCE_MAP]
         .shouldNotBeNull()
         .shouldHaveSize(1)
         .single()
         .sources
         .shouldHaveSize(1)
         .single()

      val actualSourceMap = jacksonObjectMapper().readValue<Map<String, Any>>(sourceMap.content)
      val mappedTypes = actualSourceMap["types"] as Map<String, Any>
      mappedTypes.shouldBe(
         mapOf(
            "foo.AddressBook" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.addressbook.People" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.PersonName" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.PersonId" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.EmailAddress" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.addressbook.people.Phones" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.addressbook.people.phones.Number" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.PhoneTypeEnum" to "avro/addressBookWithTaxiAnnotations.avsc",
            "foo.addressbook.people.LastUpdated" to "avro/addressBookWithTaxiAnnotations.avsc",
            "movies.Film" to "avro/fillms.avsc",
            "movies.FilmTitle" to "avro/fillms.avsc",
            "movies.ReleaseYear" to "avro/fillms.avsc",
            "movies.Genre" to "avro/fillms.avsc",
            "movies.Rating" to "avro/fillms.avsc"
         )
      )

   }

//    This isn't working.
//    It looks like the Avro generator is generating types for everything it sees.
//   @Test
//   fun `avro with invalid source causes compilation failure`() {
//      val source = loadSourcePackage("mixed-sources/invalid-avro")
//      val schema = TaxiSchema.from(source)
//      schema.hasType("foo.AddressBook")
//         .shouldBeTrue()
//      schema.type("foo.addressbook.People")
//         .attribute("email")
//         .type.shouldBe("foo.EmailAddress".fqn())
//   }

   private fun loadSourcePackage(path: String): SourcePackage {
      val loader = loader(path)
      val converter = TaxiSchemaSourcesAdaptor()
      val metadata = converter.buildMetadata(loader)
         .block()!!
      val source = converter.convert(metadata, loader).block()
      return source
   }

   private fun loader(path: String): FileSystemPackageLoader {
      val projectRoot = Resources.getResource(path)
         .toURI()
      val spec = FileProjectSpec(
         path = projectRoot.toPath()
      )
      val fileMonitor: ReactiveFileSystemMonitor = mock { }
      val fileSystemEventSink = Sinks.many().unicast().onBackpressureBuffer<List<FileSystemChangeEvent>>()
      whenever(fileMonitor.startWatching()).thenReturn(fileSystemEventSink.asFlux())
      val loader = FileSystemPackageLoader(
         spec, TaxiSchemaSourcesAdaptor(),
         fileMonitor
      )
      return loader
   }
}
