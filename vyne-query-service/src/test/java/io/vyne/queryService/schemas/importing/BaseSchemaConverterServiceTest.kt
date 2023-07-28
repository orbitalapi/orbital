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


package io.vyne.queryService.schemas.importing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.Resources
import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.ParsedPackage
import io.vyne.UriSafePackageIdentifier
import io.vyne.cockpit.core.schemas.editor.LocalSchemaEditingService
import io.vyne.cockpit.core.schemas.importing.CompositeSchemaImporter
import io.vyne.cockpit.core.schemas.importing.SchemaConverter
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.consumer.SimpleSchemaStore
import io.vyne.schemaServer.core.editor.SchemaEditorService
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.packages.PackageWithDescription
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.packages.SourcePackageDescription
import io.vyne.schemas.PartialSchema
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
         ReactiveRepositoryManager.testWithFileRepo(
            tempFolder.root.toPath(),
            isEditable = true
         ),
         schemaStore
      )
      val editingService = LocalSchemaEditingService(
         object : PackagesServiceApi {
            override fun listPackages(): Mono<List<SourcePackageDescription>> {
               return Mono.just(listOf())
            }

            override fun loadPackage(packageUri: String): Mono<PackageWithDescription> {
               return Mono.just(
                  PackageWithDescription.empty(
                     PackageIdentifier.fromUriSafeId(
                        packageUri
                     )
                  )
               )
            }

            override fun getPartialSchemaForPackage(packageUri: UriSafePackageIdentifier): Mono<PartialSchema> {
               TODO("Not yet implemented")
            }

            override fun removePackage(packageUri: UriSafePackageIdentifier): Mono<Unit> {
               TODO("Not yet implemented")
            }

         },
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
