package io.vyne.schemaServer.schemaStoreConfig

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemaServer.core.packages.SchemaServerSourceManager
import org.junit.Before
import org.junit.Test

class SchemaServerSourceManagerIntegrationTest {

   lateinit var sourceManager: SchemaServerSourceManager

   @Before
   fun setup() {
      sourceManager = SchemaServerSourceManager(
         emptyList(),
         mock{},
      )
   }
   @Test
   fun `can publish over rsocket using publisher api`() {

   }

   @Test
   fun `loads configured taxi project from disk`() {

   }


   @Test
   fun `recovers from schemas with compilation errors`() {
      // Submit a series of packages:
      // Package1 - Valid
      // Package2 - Invalid
      // Package3 - Valid (but doesn't fix Package2)
      // Package4 - Valid (resolves the error in Package2).
      // Expect that when package4 is registred, that a new schema is published with the types from package2
      sourceManager.submitSources(packageOf("package1", """type Age inherits Int""")).block()!!
      // Compilation error
      sourceManager.submitSources(packageOf("package2", """type FirstName inherits Name""")).block()!!
      sourceManager.submitSources(packageOf("package3", """type Living inherits Boolean""")).block()!!
      // Still expect that Living has been registered
      sourceManager.listSchemas().block().schema.hasType("Living").should.be.`true`
      sourceManager.listSchemas().block().schema.hasType("FirstNAme").should.be.`false`
      // Resolves the compilation error from Package2
      sourceManager.submitSources(packageOf("package4", """type Name inherits String""")).block()!!

      sourceManager.listSchemas().block().schema.hasType("Name").should.be.`true`
      sourceManager.listSchemas().block().schema.hasType("FirstName").should.be.`true`
   }

   private fun packageOf(packageName: String, content: String): SourcePackage {
      return SourcePackage(
         PackageMetadata.from("com.test", packageName),
         listOf(VersionedSource.sourceOnly(content))
      )
   }

   @Test
   fun `when compilation errors exist subsequent submissions are permitted`() {

   }

}
