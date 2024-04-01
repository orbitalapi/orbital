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


package com.orbitalhq.queryService.schemas.importing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.cockpit.core.schemas.editor.LocalSchemaEditingService
import com.orbitalhq.cockpit.core.schemas.importing.CompositeSchemaImporter
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConverter
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaServer.packages.PackageWithDescription
import com.orbitalhq.schemaServer.packages.SourcePackageDescription
import com.orbitalhq.schemas.PartialSchema
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reactor.core.publisher.Mono
import java.io.File

abstract class BaseSchemaConverterServiceTest {

   @Rule
   @JvmField
   val tempFolder = TemporaryFolder()

   fun createConverterService(
      converter: SchemaConverter<out Any>,
      projectName: String = "sample-project",
      schemaProvider: SchemaProvider
   ): CompositeSchemaImporter {
      val schemaStore = SimpleSchemaStore().setSchemaSet(SchemaSet.from(schemaProvider.schema, 0))
      return createConverterService(converter, projectName, schemaStore)
   }

   fun createConverterService(
      converter: SchemaConverter<out Any>,
      projectName: String = "sample-project",
      schemaStore: SchemaStore = SimpleSchemaStore()
   ): CompositeSchemaImporter {
      copySampleProjectTo(tempFolder.root, projectName)
      val schemaEditorService = SchemaEditorService(
         ReactiveProjectStoreManager.testWithFileRepo(
            tempFolder.root.toPath(),
            isEditable = true
         ),
         schemaStore
      )
      val mockPackageService = mock<PackageService> {
         on { listPackages() } doReturn Mono.just(listOf())
         on { loadPackage(any()) } doAnswer { invocation ->
            Mono.just(
               PackageWithDescription.empty(
                  PackageIdentifier.fromUriSafeId(invocation.arguments[0] as String)
               )
            )
         }
      }
      val editingService = LocalSchemaEditingService(
         mockPackageService,
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

fun copySampleProjectTo(target: File, projectName: String = "sample-project") {
   val testProject = File(Resources.getResource(projectName).toURI())
   FileUtils.copyDirectory(testProject, target)
}
