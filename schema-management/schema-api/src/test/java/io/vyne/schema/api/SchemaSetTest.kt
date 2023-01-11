package io.vyne.schema.api

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.should
import io.vyne.*
import io.vyne.serde.TaxiJacksonModule
import org.junit.Test

class SchemaSetTest {

   @Test
   fun `providing another schema with the same identifier replaces existing version`() {
      val schemaSet = SchemaSet.EMPTY
      val packages = schemaSet.getPackagesAfterUpdate(
         SourcePackage(
            packageMetadata = PackageMetadata.from("com.acme", "films", "1.0.0"),
            emptyList()
         )
      )

      packages.shouldContainExactly(PackageIdentifier("com.acme", "films", "1.0.0"))

      schemaSet.getPackagesAfterUpdate(
         SourcePackage(
            packageMetadata = PackageMetadata.from("com.acme", "films", "2.0.0"),
            emptyList()
         )
      ).shouldContainExactly(
         PackageIdentifier("com.acme", "films", "2.0.0")
      )
   }


   @Test
   fun `can add shcema with different id`() {
      val schemaSet = SchemaSet.fromParsed(
         listOf(
            ParsedPackage(
               PackageMetadata.from("com.acme", "films", "1.0.0"),
               emptyList()
            )
         ),
         1
      )

      val result = schemaSet.getPackagesAfterUpdate(
         SourcePackage(
            packageMetadata = PackageMetadata.from("com.acme", "actors", "2.0.0"),
            emptyList()
         )
      )
      result.shouldContainExactly(
         PackageIdentifier("com.acme", "films", "1.0.0"),
         PackageIdentifier("com.acme", "actors", "2.0.0"),
      )
   }

   @Test
   fun `can remove schema`() {
      val schemaSet = SchemaSet.fromParsed(
         listOf(
            ParsedPackage(PackageMetadata.from("com.acme", "films", "1.0.0"), emptyList()),
            ParsedPackage(PackageMetadata.from("com.acme", "actors", "2.0.0"), emptyList())
         ),
         1
      )

      val result = schemaSet.getPackagesAfterUpdate(null, listOf(PackageIdentifier("com.acme", "actors", "2.0.0")))
      result.shouldContainExactly(
         PackageIdentifier("com.acme", "films", "1.0.0"),
      )

   }

   @Test
   fun `can read and write a schemaset to-from json`() {
      val schemaSet = SchemaSet.fromParsed(
         listOf(
            ParsedPackage(PackageMetadata.from("com.acme", "films", "1.0.0"),listOf(ParsedSource(VersionedSource.sourceOnly(
               """model HelloWorld {
                  | firstName : String
                  | lastName : String
                  | fullName : concat(this.firstName, this.lastName)
                  |}
               """.trimMargin()
            )))),
         ),
         generation = 1
      )

      val jackson = jacksonObjectMapper()
         .findAndRegisterModules()
         .registerModule(TaxiJacksonModule)
      val json = jackson.writerWithDefaultPrettyPrinter().writeValueAsString(schemaSet)
      val fromJson = jackson.readValue<SchemaSet>(json)
      fromJson.schema.taxi.should.equal(schemaSet.schema.taxi)
   }

   @Test
   fun `can read and write a schemaset to-from cbor`() {
      val schemaSet = SchemaSet.fromParsed(
         listOf(
            ParsedPackage(PackageMetadata.from("com.acme", "films", "1.0.0"),listOf(ParsedSource(VersionedSource.sourceOnly(
               """model HelloWorld {
                  | firstName : String
                  | lastName : String
                  | fullName : concat(this.firstName, this.lastName)
                  |}
               """.trimMargin()
            )))),
         ),
         generation = 1
      )

      val mapper = CBORMapper.builder().findAndAddModules().build()
      val cbor = mapper.writeValueAsBytes(schemaSet)
      val fromCbor = mapper.readValue<SchemaSet>(cbor)
      fromCbor.schema.taxi.should.equal(schemaSet.schema.taxi)
   }



   private fun List<SourcePackage>.shouldContainExactly(vararg identifiers: PackageIdentifier) {
      this.size.should.equal(identifiers.size)
      identifiers.forEach { identifier ->
         this.any { it.identifier == identifier }.should.be.`true`
      }
   }
}
