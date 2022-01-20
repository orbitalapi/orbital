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
//package io.vyne.queryService.schemas.importing
//
//import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
//import com.google.common.io.Resources
//import io.vyne.queryService.schemas.editor.LocalSchemaEditingService
//import io.vyne.schemaServer.editor.DefaultApiEditorRepository
//import io.vyne.schemaServer.editor.SchemaEditorService
//import io.vyne.schemaServer.file.FileSystemSchemaRepository
//import io.vyne.schemaStore.SimpleSchemaStore
//import org.apache.commons.io.FileUtils
//import org.junit.Rule
//import org.junit.rules.TemporaryFolder
//import java.io.File
//
//abstract class BaseSchemaConverterServiceTest {
//
//   @Rule
//   @JvmField
//   val tempFolder = TemporaryFolder()
//
//   fun createConverterService(converter: SchemaConverter<out Any>): CompositeSchemaImporter {
//      copySampleProjectTo(tempFolder.root)
//      val schemaStore = SimpleSchemaStore()
//      val schemaEditorService = SchemaEditorService(
//         DefaultApiEditorRepository(
//            FileSystemSchemaRepository.forPath(tempFolder.root.toPath())
//         )
//      )
//      val editingService: LocalSchemaEditingService = LocalSchemaEditingService(
//         schemaEditorService,
//         schemaStore
//      )
//      return CompositeSchemaImporter(
//         listOf(converter),
//         editingService,
//         jacksonObjectMapper()
//      )
//
//   }
//
//
//}
//
//fun copySampleProjectTo(target:File) {
//   val testProject = File(Resources.getResource("sample-project").toURI())
//   FileUtils.copyDirectory(testProject, target)
//}
