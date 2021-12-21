package io.vyne.queryService.schemas.importing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import io.vyne.queryService.schemas.editor.LocalSchemaEditingService
import io.vyne.schemaServer.editor.DefaultApiEditorRepository
import io.vyne.schemaServer.editor.SchemaEditorService
import io.vyne.schemaServer.file.FileSystemSchemaRepository
import io.vyne.schemaStore.SimpleSchemaStore
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class BaseSchemaConverterServiceTest {

   @Rule
   @JvmField
   val tempFolder = TemporaryFolder()

   fun createConverterService(converter: SchemaConverter<out Any>): CompositeSchemaImporter {
      copySampleProjectTo(tempFolder.root)
      val schemaStore = SimpleSchemaStore()
      val schemaEditorService = SchemaEditorService(
         DefaultApiEditorRepository(
            FileSystemSchemaRepository.forPath(tempFolder.root.toPath())
         )
      )
      val editingService: LocalSchemaEditingService = LocalSchemaEditingService(
         schemaEditorService,
         schemaStore
      )
      return CompositeSchemaImporter(
         listOf(converter),
         editingService,
         jacksonObjectMapper()
      )

   }


}

fun copySampleProjectTo(target:File) {
   val testProject = File(Resources.getResource("sample-project").toURI())
   FileUtils.copyDirectory(testProject, target)
}
