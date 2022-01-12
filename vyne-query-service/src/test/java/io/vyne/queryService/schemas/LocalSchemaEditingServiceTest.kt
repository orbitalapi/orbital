// In order to make these tests easier, they depend on classes in
// schema-server.  However, that project is wired as a spring-boot runnable
// jar, so cannot be used as a dependency in other projects.
// The solution is to move some classes around.
// We ultimately plan on splitting the schema-server-runtime and schema-server-library,
// so that the server can be embedded.
// That will give these tests a library they can depend on, which will make
// them work.
// But, right now there's a very large refactor going on in the schema-server.
// So, Imma leave these tests commented out until that's merged, and
// then move stuff around and re-enable these.

//
//

//package io.vyne.queryService.schemas
//
//import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//import com.google.common.io.Resources
//import com.winterbe.expekt.should
//import io.vyne.VersionedSource
//import io.vyne.queryService.schemas.editor.EditedSchema
//import io.vyne.queryService.schemas.editor.LocalSchemaEditingService
//import io.vyne.queryService.withoutWhitespace
//import io.vyne.schemaServer.editor.DefaultApiEditorRepository
//import io.vyne.schemaServer.editor.SchemaEditorService
//import io.vyne.schemaServer.file.FileSystemSchemaRepository
//import io.vyne.schemaServer.file.FileSystemVersionedSourceLoader
//import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
//import io.vyne.schemas.taxi.TaxiSchema
//import io.vyne.spring.FileSystemSchemaLoader
//import lang.taxi.errors
//import org.apache.commons.io.FileUtils
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.rules.TemporaryFolder
//import java.io.File
//import java.time.Duration
//import kotlin.test.assertFailsWith
//
//class LocalSchemaEditingServiceTest {
//
//   lateinit var editorService: LocalSchemaEditingService
//
//   @Rule
//   @JvmField
//   val projectHome = TemporaryFolder()
//
//   lateinit var schemaStore: LocalValidatingSchemaStoreClient
//
//   @Before
//   fun setup() {
//      copyProject("sample-project")
//      schemaStore = LocalValidatingSchemaStoreClient()
//      schemaStore.submitSchemas(BuiltInTypesProvider.versionedSources)
//      editorService = LocalSchemaEditingService(
//         SchemaEditorService(
//            DefaultApiEditorRepository(
//               FileSystemSchemaRepository(
//                  FileSystemVersionedSourceLoader(
//                     FileSystemSchemaLoader(projectHome.root.toPath())
//                  )
//               )
//            )
//         ),
//         schemaStore,
//      )
//   }
//
//   @Test
//   fun `can write a schema that updates existing types`() {
//      // First, create the types
//      val originalSchema = VersionedSource.unversioned(
//         "originalSource", """
//            namespace foo.test
//
//         type FirstName inherits String
//         type LastName inherits String
//
//         model Customer {
//            firstName : FirstName
//         }
//      """.trimIndent()
//      )
//      schemaStore.submitSchema(originalSchema)
//
//      // Now submit an edit
//      // Note - the easiest way to build an edit request is to compile
//      // the state we are editing to, then submit that.
//      val editedSource = """
//         namespace foo.test
//
//         type FirstName inherits String
//         type LastName inherits String
//
//         [[ This is some docs ]]
//         @AnnotationGoesHere
//         model Customer {
//            firstName : FirstName
//            lastName : LastName
//         }
//      """.trimIndent()
//      val editedTaxi = TaxiSchema.from(editedSource)
//      val editResult = editorService.submitEditedSchema(
//         EditedSchema(
//            types = setOf(editedTaxi.type("foo.test.Customer"))
//         )
//      ).block(Duration.ofSeconds(1))
//      editResult.messages.errors().should.be.empty
//      val updatedSourceFile = projectHome.root.resolve("src/foo/test/Customer.taxi")
//      val expected = """import foo.test.FirstName
//import foo.test.LastName
//namespace foo.test {
//   [[ This is some docs ]]
//      @AnnotationGoesHere
//      model Customer {
//         firstName : FirstName
//         lastName : LastName
//      }
//}"""
//      val actualUpdatedSource = updatedSourceFile.readText()
//      actualUpdatedSource.should.equal(expected)
//   }
//
//
//   @Test
//   fun `can submit an edited schema`() {
//      val editedSchema: EditedSchema = jacksonObjectMapper()
//         .readValue(SubmitEditJson.JSON)
//      val edit = editorService.submitEditedSchema(editedSchema)
//         .block(Duration.ofSeconds(1))
//
//      edit.services.should.have.size(1)
//      edit.services[0].queryOperations.should.have.size(1)
//      edit.types.should.have.size(11)
//      edit.messages.should.be.empty
//   }
//
//   @Test
//   fun `can publish a new type`() {
//      val result = editorService.submit(
//         """
//         type Name inherits String
//      """.trimIndent()
//      ).block(Duration.ofSeconds(1))!!
//      result.types.should.have.size(1)
//   }
//
//   @Test
//   fun `can publish formatted types`() {
//      val taxi = """
//         type BirthDate inherits Date(@format = "dd/MON/yyyy")
//      """.trimIndent()
//      editorService.submit(
//         taxi
//      )
//      val expectedCreated = projectHome.root.toPath()
//         .resolve("src/BirthDate.taxi")
//         .toFile()
//
//      expectedCreated.exists().should.be.`true`
//      val generatedSource = expectedCreated.readText()
//      generatedSource.should.equal(
//         """type BirthDate inherits Date(@format = "dd/MON/yyyy")"""
//      )
//   }
//
////   Leaving this to the update mechanism that comes back from the schema server
////   @Test
////   fun `published types are present in the schemaStore`() {
////      editorService.submit(
////         """
////         type BirthDate inherits Date(@format = "dd/MON/yyyy")
////      """.trimIndent()
////      )
////      val type = schemaStore.schemaSet().schema.type("BirthDate")
////      type.should.not.be.`null`
////   }
//
//   @Test
//   fun `can publish a type which references existing types in the schema`() {
//      schemaStore.submitSchema(
//         "baseSchema", "0.1.0", """
//         namespace vyne.core.names
//         type Name inherits String"""
//      )
//      val types = editorService.submit(
//         """
//         import vyne.core.names.Name
//
//         namespace vyne.test.names
//
//         type FirstName inherits Name
//      """.trimIndent()
//      ).block(Duration.ofSeconds(1))!!.types
//      types.should.have.size(1)
//      types[0].fullyQualifiedName.should.equal("vyne.test.names.FirstName")
//
//      val expectedCreated = projectHome.root.toPath()
//         .resolve("src/vyne/test/names/FirstName.taxi")
//         .toFile()
//
//      expectedCreated.exists().should.be.`true`
//      val generatedSource = expectedCreated.readText()
//      val expected = """import vyne.core.names.Name
//namespace vyne.test.names {
//   type FirstName inherits Name
//}"""
//      generatedSource.withoutWhitespace().should.equal(
//         expected.withoutWhitespace()
//      )
//   }
//
//   @Test
//   fun `can update a type`() {
//      // First submission creates
//      editorService.submit("""type BirthDate inherits Date""")
//      // Now edit
//      editorService.submit(
//         """
//         type BirthDate inherits Date(@format = "dd/MON/yyyy")
//      """
//      )
////      val type = schemaStore.schemaSet().schema.type("BirthDate")
////      type.should.not.be.`null`
////      type.format!![0].should.equal("dd/MON/yyyy")
//      val expectedCreated = projectHome.root.toPath()
//         .resolve("src/BirthDate.taxi")
//         .toFile()
//
//      expectedCreated.exists().should.be.`true`
//      val generatedSource = expectedCreated.readText()
//      generatedSource.should.equal(
//         """type BirthDate inherits Date(@format = "dd/MON/yyyy")"""
//      )
//   }
//
//   @Test
//   fun `publishing a new type which doesnt compile is rejected`() {
//      val exception = assertFailsWith<io.vyne.spring.http.BadRequestException> { editorService.submit("""type Foo inherits""") }
//      exception.message.should.equal("missing Identifier at '<EOF>'")
//   }
//
//   @Test
//   fun `updating a type which is not stored in a mutating source gets rejected`() {
//      // No idea how to do this one ....
//   }
//
//
//   private fun copyProject(path: String) {
//      val testProject = File(Resources.getResource(path).toURI())
//      FileUtils.copyDirectory(testProject, projectHome.root)
//   }
//}
