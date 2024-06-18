package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.ParsedPackage
import com.orbitalhq.ParsedSource
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.schemas.BuiltInTypesProvider
import com.orbitalhq.cockpit.core.schemas.editor.LocalSchemaEditingService
import com.orbitalhq.cockpit.core.schemas.editor.SchemaSubmissionResult
import com.orbitalhq.schema.publisher.PublisherHealth
import com.orbitalhq.schema.publisher.PublisherType
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.packages.PackageWithDescription
import com.orbitalhq.schemaServer.packages.SourcePackageDescription
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import org.junit.jupiter.api.BeforeEach
import reactor.core.publisher.Mono
import java.time.Instant

abstract class BaseSchemaEditOperationTest {
   protected lateinit var editorService: LocalSchemaEditingService
   lateinit var schemaStore: LocalValidatingSchemaStoreClient

   protected val schemaEditorApi = mock<SchemaEditorService> { }
   protected val packagesServiceApi = mock<PackageService> { }
   protected val objectMapper = jacksonObjectMapper()

   @BeforeEach
   fun setup() {
      schemaStore = LocalValidatingSchemaStoreClient()
      schemaStore.submitPackage(
         SourcePackage(
            BuiltInTypesProvider.sourcePackage.packageMetadata,
            BuiltInTypesProvider.sourcePackage.sources
         )
      )
      editorService = LocalSchemaEditingService(packagesServiceApi, schemaEditorApi, schemaStore)
   }

   protected fun stubPackageApiToReturnEmptyPackage() {
      whenever(packagesServiceApi.loadPackage(any())).thenReturn(
         Mono.just(
            sourcePackageOf("").withDescription()
         )
      )
   }

   protected fun stubPackageApiToReturnPackage(source: String) {
      whenever(packagesServiceApi.loadPackage(any())).thenReturn(
         Mono.just(
            sourcePackageOf(source).withDescription()
         )
      )
   }

   private fun sourcePackageOf(
      source: String,
      packageIdentifier: PackageIdentifier = PackageIdentifier.fromId("test/test/1.0.0")
   ): SourcePackage {
      return SourcePackage(
         PackageMetadata.from(packageIdentifier),
         listOf(
            VersionedSource(
               "TestSrc", "1.0.0", source
            )
         )
      )
   }

   protected fun applyEdit(
      filename: String,
      baseline: String,
      vararg changes: SchemaEditOperation
   ): SchemaSubmissionResult {
      val packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
      val edit = SchemaEdit(
         packageIdentifier,
         listOf(
            CreateOrReplaceSource(
               listOf(
                  VersionedSource(
                     filename,
                     "1.0.0",
                     baseline,
                  )
               )
            )
         ) + changes.toList(),
      )
      return editorService.submitSchemaEditOperation(edit).block()!!
   }

}

fun SourcePackage.withDescription(editable: Boolean = true): PackageWithDescription {
   return PackageWithDescription(
      this.parsed(),
      SourcePackageDescription(
         identifier = this.identifier,
         health = PublisherHealth(PublisherHealth.Status.Healthy),
         sourceCount = 1,
         warningCount = 0,
         errorCount = 0,
         publisherType = PublisherType.FileSystem,
         editable = editable,
         submissionDate = Instant.now(),
         packageConfig = null
      )
   )
}


fun SourcePackage.parsed(): ParsedPackage {
   return ParsedPackage(
      this.packageMetadata,
      this.sources.map { ParsedSource(it) },
      emptyMap()
   )
}
