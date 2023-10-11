package com.orbitalhq.cockpit.core.schemas.importing

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.schemas.editor.LocalSchemaEditingService
import com.orbitalhq.cockpit.core.schemas.editor.SchemaSubmissionResult
import com.orbitalhq.cockpit.core.schemas.editor.operations.CreateOrReplaceSource
import com.orbitalhq.cockpit.core.schemas.editor.operations.SchemaEdit
import com.orbitalhq.utils.Ids
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.Message
import lang.taxi.generators.hasErrors
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

@Component
class CompositeSchemaImporter(
   private val importers: List<SchemaConverter<out Any>>,
   private val schemaEditor: LocalSchemaEditingService,
   private val objectMapper: ObjectMapper
) {
   private val logger = KotlinLogging.logger {}

   init {
      logger.info { "Found ${importers.size} schema converters:  ${importers.joinToString { it::class.simpleName!! }}" }
   }

   fun preview(request: SchemaConversionRequest): Mono<SchemaSubmissionResult> {
      return doConversion(request, validateOnly = true)
   }

   private fun doConversion(request: SchemaConversionRequest, validateOnly: Boolean): Mono<SchemaSubmissionResult> {
      val importer = findImporter(request.format)
      // Only convert if we got something other than what we asked for.
      // In tests, we typically pass the correct type, and trying to run that through a
      // converter can cause problems (eg., using @JsonDeserialize annotations)
      val options = if (request.options::class == importer.conversionParamsType) {
         request.options
      } else {
         objectMapper.convertValue(request.options, importer.conversionParamsType.java)
      }



      return Mono.zip(
         importer.convert(request, options),
         schemaEditor.getSourcePackage(request.packageIdentifier)
      )
         .flatMap { tuple2 ->
            val sourcePackageWithMessages = tuple2.t1
            val existingSourcePackage = tuple2.t2

            // TODO : Convert generated sources to SourcePackage
            schemaEditor.submitSchemaEditOperation(
               SchemaEdit(
                  existingSourcePackage.identifier,
                  listOf(CreateOrReplaceSource(sourcePackageWithMessages.sourcePackage.sources)),
                  dryRun = validateOnly
               )

            )


//            schemaEditor.submit(
//               sourcePackageWithMessages.concatenatedSource,
//               validateOnly,
//               editorConfig.editablePackages.single().id
//            )
         }


   }

   fun import(request: SchemaConversionRequest): Mono<SchemaSubmissionResult> {
      return doConversion(request, validateOnly = false)
   }

   private fun findImporter(format: String): SchemaConverter<Any> {
      return importers.firstOrNull { it.supportedFormats.contains(format) } as SchemaConverter<Any>?
         ?: error("No importer found for format $format")
   }
}

interface SchemaConverter<TConversionParams : Any> {
   val supportedFormats: List<String>
   val conversionParamsType: KClass<TConversionParams>

   /**
    * Converts a schema to Taxi
    */
   fun convert(request: SchemaConversionRequest, options: TConversionParams): Mono<SourcePackageWithMessages>
}

data class SchemaConversionRequest(
   val format: String,
   val options: Any = emptyMap<String, Any>(),
   val packageIdentifier: PackageIdentifier
)

/**
 * The result of a generation / edit operation.
 * Contains the generated code (wrapped in a source package), along with
 * any messages from the generators.
 */
data class SourcePackageWithMessages(
   val sourcePackage: SourcePackage,
   val messages: List<Message>
)

// Used for testing, to be backwards compatible with what came before
val SourcePackageWithMessages.concatenatedSource: String
   get() {
      return this.sourcePackage.sources.joinToString("\n") { it.content }
   }

// Used for testing, to be backwards compatible with what came before
val SourcePackageWithMessages.hasErrors: Boolean
   get() {
      return this.messages.hasErrors()
   }

fun GeneratedTaxiCode.toSourcePackageWithMessages(
   identifier: PackageIdentifier,
   baseFileName: String
): SourcePackageWithMessages {
   return SourcePackageWithMessages(
      messages = this.messages,
      sourcePackage = SourcePackage(
         PackageMetadata.from(identifier),
         sources = this.taxi.mapIndexed { idx, source ->
            val fileName = if (idx == 0) "${baseFileName}.taxi" else "${baseFileName}${idx}.taxi"
            VersionedSource(
               fileName,
               identifier.version,
               source
            )
         },
         emptyMap()
      ),
   )
}


fun generatedImportedFileName(baseName: String): String {
   return baseName + Ids.id("Imported-", 4)
}
