// This code depends on schema-server, which seems not to be allowing dependencies.
// (Possibly because of spring boot jar rewriting)
// Commenting out to un-break build whilst we think of a different strategy.
//

package io.vyne.queryService.schemas.editor

//
//import io.vyne.schemaServer.file.FileSystemSchemaRepository
import arrow.core.getOrHandle
import io.vyne.VersionedSource
import io.vyne.queryService.BadRequestException
import io.vyne.schemaServer.editor.SchemaEditRequest
import io.vyne.schemaServer.editor.SchemaEditResponse
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.CompilationError
import lang.taxi.CompilationMessage
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.errors
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Compiled
import lang.taxi.types.ImportableToken
import lang.taxi.types.ObjectType
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import kotlin.random.Random

data class TaxiSubmissionResult(
   val types: List<Type>,
   val services: List<Service>,
   val messages: List<CompilationMessage>,
   val taxi: String
)

//
//
//data class TaxiSubmissionResponse(
//   val messages: List<CompilationError>,
//   val updatedTypes: List<Type>
//)
//
//@ConditionalOnProperty("vyne.schema.localStore")
@RestController
class LocalSchemaEditingService(
   private val schemaEditorApi: SchemaEditorApi,
   private val schemaStore: SchemaStore,
   private val schemaPublisher: SchemaPublisher
) {

   /**
    * Submit a taxi string containing schema changes.
    * Updates are persisted to the local schema repository, and then published to
    * the schema store.
    *
    * The updated Vyne types containing in the Taxi string are returned.
    */
   @PostMapping("/api/schema/taxi", consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE])
   fun submit(
      @RequestBody taxi: String,
      @RequestParam("validate", required = false) validateOnly: Boolean = false
   ): Mono<TaxiSubmissionResult> {
      val importRequestSourceName = "ImportRequest" + Random.nextInt()
      val (messages, compiled) = validate(taxi, importRequestSourceName)
      val typesInThisRequest = getCompiledElementsInSources(compiled.types, importRequestSourceName)
      val servicesInThisRequest = getCompiledElementsInSources(compiled.services, importRequestSourceName)

      val generatedThings: List<Pair<ImportableToken, List<CompilationUnit>>> =
         typesInThisRequest + servicesInThisRequest
      val (updatedSchema, versionedSources) = toVersionedSources(generatedThings)
      val persist = !validateOnly
      val vyneTypes = typesInThisRequest.map { (type, _) -> updatedSchema.type(type) }
      val vyneServices = servicesInThisRequest.map { (service, _) -> updatedSchema.service(service.qualifiedName) }
      val submissionResult = TaxiSubmissionResult(
         vyneTypes, vyneServices, messages, taxi
      )
      return if (persist) {
         submitEdits(versionedSources)
            .map { submissionResult }
      } else {
         Mono.just(submissionResult)
      }
   }

   fun submitEdits(versionedSources: List<VersionedSource>): Mono<SchemaEditResponse> {
      log().info("Submitting edit requests to schema server for files ${versionedSources.joinToString(", ") { it.name }}")
      return schemaEditorApi.submitEdits(SchemaEditRequest(versionedSources))
   }

   private fun <T : Compiled> getCompiledElementsInSources(
      compiled: Set<T>,
      sourceName: String
   ): List<Pair<T, List<CompilationUnit>>> {
      return compiled
         .mapNotNull { type ->
            val compilationUnitsInThisOperation =
               type.compilationUnits.filter { compilationUnit -> compilationUnit.source.sourceName == sourceName }
            if (compilationUnitsInThisOperation.isNotEmpty()) {
               type to compilationUnitsInThisOperation
            } else {
               null
            }
         }
   }

   //
   private fun validate(taxi: String, importRequestSourceName: String): Pair<List<CompilationError>, TaxiDocument> {
      val importSources = schemaStore.schemaSet().taxiSchemas
         .map { it.document }

      // First we pre-validate with the compiler.
      // We need to do this, as we want to ensure the content is valid before writing to disk as VersionedSources.
      val (messages, compiled) = Compiler(taxi, sourceName = importRequestSourceName, importSources = importSources)
         .compileWithMessages()
      if (messages.errors().isNotEmpty()) {
         throw BadRequestException(messages.errors().joinToString("\n") { it.detailMessage })
      }
      return messages to compiled
   }

   private fun toVersionedSources(typesAndSources: List<Pair<lang.taxi.types.ImportableToken, List<CompilationUnit>>>): Pair<Schema, List<VersionedSource>> {
      // We have to work out a Type-to-file strategy.
      // As a first pass, I'm using a seperate file for each type.
      // It's a little verbose on the file system, but it's a reasonable start, as it makes managing edits easier, since
      // we don't have to worry about insertions / modification within the middle of a file.
      val versionedSources = typesAndSources.map { (type, compilationUnits) ->
         val source = compilationUnits.joinToString("\n") { it.source.content }//reconstructSource(type, compilationUnits)
         VersionedSource(
            type.qualifiedName.replace(".", "/") + ".taxi",
            VersionedSource.DEFAULT_VERSION.toString(),
            source
         )
      }
      val schema = schemaPublisher.validate(versionedSources).getOrHandle { throw it }
      return schema to versionedSources
   }


   private fun reconstructSource(
      type: lang.taxi.types.ImportableToken,
      compilationUnits: List<CompilationUnit>
   ): String {
      val imports = if (type is ObjectType) {
         type.referencedTypes
            .map { referencedType -> referencedType.formattedInstanceOfType ?: referencedType }
            .map { "import ${it.qualifiedName}" }
            .distinct()
      } else emptyList()
      val namespace = if (type.toQualifiedName().namespace.isNullOrEmpty()) {
         ""
      } else {
         "namespace ${type.toQualifiedName().namespace}\n"
      }
      return """${imports.joinToString("\n")}
$namespace
${compilationUnits.joinToString("\n") { it.source.content }}
""".trim()
   }
}