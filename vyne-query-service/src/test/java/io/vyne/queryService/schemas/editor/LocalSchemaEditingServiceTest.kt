package io.vyne.queryService.schemas.editor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.VersionedSource
import io.vyne.asPackage
import io.vyne.queryService.schemas.BuiltInTypesProvider
import io.vyne.queryService.schemas.SubmitEditJson
import io.vyne.queryService.withoutWhitespace
import io.vyne.schemaServer.core.adaptors.taxi.TaxiSchemaSourcesAdaptor
import io.vyne.schemaServer.core.editor.SchemaEditorService
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoader
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import io.vyne.schemaServer.core.file.packages.ReactivePollingFileSystemMonitor
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import io.vyne.schemaServer.core.repositories.SimpleRepositoryManager
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleManager
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.errors
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Duration
import kotlin.test.assertFailsWith

class LocalSchemaEditingServiceTest {

   lateinit var editorService: LocalSchemaEditingService

   @Rule
   @JvmField
   val projectHome = TemporaryFolder()

   lateinit var schemaStore: LocalValidatingSchemaStoreClient

   // Should match the package in the sample-project that is deployed in setup()
   val packageId = PackageIdentifier.fromId("taxi/sample/0.3.0")

   @Before
   fun setup() {
      val projectFile = copyProject("sample-project")
      val schemaEventDispatcher = RepositoryLifecycleManager()
      schemaStore = LocalValidatingSchemaStoreClient()
      schemaStore.submitPackage(BuiltInTypesProvider.versionedSources)
      editorService = LocalSchemaEditingService(
         SchemaEditorService(
            SimpleRepositoryManager(
               initialLoaders = listOf(
                  FileSystemPackageLoader(
                     config = FileSystemPackageSpec(
                        projectFile.toPath(),
                        isEditable = true
                     ),
                     adaptor = TaxiSchemaSourcesAdaptor(),
                     fileMonitor = ReactivePollingFileSystemMonitor(projectFile.toPath(), Duration.ofDays(1))
                  )
               )
            ),
            schemaStore
         ),
         schemaStore
      )
   }

   @Test
   fun `can write a schema that updates existing types`() {
      // First, create the types
      val originalSchema = VersionedSource.unversioned(
         "originalSource", """
            namespace foo.test

         type FirstName inherits String
         type LastName inherits String

         model Customer {
            firstName : FirstName
         }
      """.trimIndent()
      ).asPackage(packageId)
      schemaStore.submitPackage(originalSchema)

      // Now submit an edit
      // Note - the easiest way to build an edit request is to compile
      // the state we are editing to, then submit that.
      val editedSource = """
         namespace foo.test

         type FirstName inherits String
         type LastName inherits String

         [[ This is some docs ]]
         @AnnotationGoesHere
         model Customer {
            firstName : FirstName
            lastName : LastName
         }
      """.trimIndent()
      val editedTaxi = TaxiSchema.from(editedSource)
      val editResult = editorService.submitEditedSchema(
         EditedSchema(
            types = setOf(editedTaxi.type("foo.test.Customer"))
         ),
         originalSchema.identifier.id
      ).block(Duration.ofSeconds(1))
      editResult.messages.errors().should.be.empty
      val updatedSourceFile = projectHome.root.resolve("src/foo/test/Customer.taxi")
      val expected = """import foo.test.FirstName
import foo.test.LastName
namespace foo.test {
   [[ This is some docs ]]
      @AnnotationGoesHere
      model Customer {
         firstName : FirstName
         lastName : LastName
      }
}"""
      val actualUpdatedSource = updatedSourceFile.readText()
      actualUpdatedSource.should.equal(expected)
   }


   @Test
   fun `can submit an edited schema`() {
      val editedSchema: EditedSchema = jacksonObjectMapper()
         .readValue(SubmitEditJson.JSON)
      val edit = editorService.submitEditedSchema(editedSchema, packageId.id)
         .block(Duration.ofSeconds(1))

      edit.services.should.have.size(1)
      edit.services.first().queryOperations.should.have.size(1)
      edit.types.should.have.size(11)
      edit.messages.should.be.empty
   }

   @Test
   fun `can publish a new type`() {
      val result = editorService.submit(
         """
         type Name inherits String
      """.trimIndent(),
         rawPackageIdentifier = packageId.id
      ).block(Duration.ofSeconds(1))!!
      result.types.should.have.size(1)
   }


//   Leaving this to the update mechanism that comes back from the schema server
//   @Test
//   fun `published types are present in the schemaStore`() {
//      editorService.submit(
//         """
//         type BirthDate inherits Date(@format = "dd/MON/yyyy")
//      """.trimIndent()
//      )
//      val type = schemaStore.schemaSet().schema.type("BirthDate")
//      type.should.not.be.`null`
//   }

   @Test
   fun `can publish a type which references existing types in the schema`() {
      schemaStore.submitPackage(
         VersionedSource(
            name = "baseSchema.taxi",
            version = "0.1.0",
            content = """
                     namespace vyne.core.names
                     type Name inherits String"""
         ).asPackage(packageId)
      )
      val types = editorService.submit(
         taxi = """
                  import vyne.core.names.Name

                  namespace vyne.test.names

                  type FirstName inherits Name
               """.trimIndent(),
         rawPackageIdentifier = packageId.id
      ).block(Duration.ofSeconds(1))!!.types
      types.should.have.size(1)
      types.single().fullyQualifiedName.should.equal("vyne.test.names.FirstName")

      val expectedCreated = projectHome.root.toPath()
         .resolve("src/vyne/test/names/FirstName.taxi")
         .toFile()

      expectedCreated.exists().should.be.`true`
      val generatedSource = expectedCreated.readText()
      val expected = """import vyne.core.names.Name
namespace vyne.test.names {
   type FirstName inherits Name
}"""
      generatedSource.withoutWhitespace().should.equal(
         expected.withoutWhitespace()
      )
   }

   @Test
   fun `can update a type`() {
      // First submission creates
      editorService.submit("""type BirthDate inherits Date""", rawPackageIdentifier = packageId.id)
      // Now edit
      editorService.submit(
         """
            @Format("dd/MON/yyyy")
         type BirthDate inherits Date
      """, rawPackageIdentifier = packageId.id
      )
//      val type = schemaStore.schemaSet().schema.type("BirthDate")
//      type.should.not.be.`null`
//      type.format!![0].should.equal("dd/MON/yyyy")
      val expectedCreated = projectHome.root.toPath()
         .resolve("src/BirthDate.taxi")
         .toFile()

      expectedCreated.exists().should.be.`true`
      val generatedSource = expectedCreated.readText()
      generatedSource.should.equal(
         """type BirthDate inherits Date(@format = "dd/MON/yyyy")"""
      )
   }

   @Test
   fun `publishing a new type which doesnt compile is rejected`() {
      val exception =
         assertFailsWith<io.vyne.spring.http.BadRequestException> {
            editorService.submit(
               """type Foo inherits""",
               rawPackageIdentifier = packageId.id
            )
         }
      exception.message.should.equal("missing Identifier at '<EOF>'")
   }

   @Test
   fun `updating a type which is not stored in a mutating source gets rejected`() {
      // No idea how to do this one ....
   }


   private fun copyProject(path: String): File {
      val testProject = File(Resources.getResource(path).toURI())
      FileUtils.copyDirectory(testProject, projectHome.root)
      return projectHome.root
   }
}
