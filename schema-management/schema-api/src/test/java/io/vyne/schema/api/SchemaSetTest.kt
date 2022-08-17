package io.vyne.schema.api

import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.ParsedPackage
import io.vyne.SourcePackage
import org.junit.Assert.*
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

   private fun List<SourcePackage>.shouldContainExactly(vararg identifiers: PackageIdentifier) {
      this.size.should.equal(identifiers.size)
      identifiers.forEach { identifier ->
         this.any { it.identifier == identifier }.should.be.`true`
      }
   }
}
