// This code depends on schema-server, which seems not to be allowing dependencies.
// (Possibly because of spring boot jar rewriting)
// Commenting out to un-break build whilst we think of a different strategy.
//

//package io.vyne.queryService.schemas.editor
//
//import arrow.core.getOrHandle
//import io.vyne.VersionedSource
//import io.vyne.queryService.BadRequestException
//import io.vyne.schemaServer.file.FileSystemSchemaRepository
//import io.vyne.schemaStore.SchemaPublisher
//import io.vyne.schemaStore.SchemaStore
//import io.vyne.schemas.Type
//import lang.taxi.CompilationError
//import lang.taxi.Compiler
//import lang.taxi.TaxiDocument
//import lang.taxi.errors
//import lang.taxi.types.CompilationUnit
//import lang.taxi.types.ObjectType
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
//import org.springframework.http.MediaType
//import org.springframework.web.bind.annotation.PostMapping
//import org.springframework.web.bind.annotation.RequestBody
//import org.springframework.web.bind.annotation.RestController
//import kotlin.random.Random
//
//
//data class TaxiSubmissionResponse(
//   val messages: List<CompilationError>,
//   val updatedTypes: List<Type>
//)
//
//@ConditionalOnProperty("vyne.schema.localStore")
//@RestController
//class LocalSchemaEditingService(
//   private val schemaRepository: FileSystemSchemaRepository,
//   private val schemaStore: SchemaStore,
//   private val schemaPublisher: SchemaPublisher
//) {
//
//   /**
//    * Submit a taxi string containing schema changes.
//    * Updates are persisted to the local schema repository, and then published to
//    * the schema store.
//    *
//    * The updated Vyne types containing in the Taxi string are returned.
//    */
//   @PostMapping("/api/schema/types", consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE])
//   fun submit(@RequestBody taxi: String): List<Type> {
//      val importRequestSourceName = "ImportRequest" + Random.nextInt()
//      val (messages, compiled) = validate(taxi, importRequestSourceName)
//      val sourcesFromThisImport = compiled.types
//         .mapNotNull { type ->
//            val compilationUnitsInThisOperation =
//               type.compilationUnits.filter { compilationUnit -> compilationUnit.source.sourceName == importRequestSourceName }
//            if (compilationUnitsInThisOperation.isNotEmpty()) {
//               type to compilationUnitsInThisOperation
//            } else {
//               null
//            }
//         }
//      val versionedSource = persistSources(sourcesFromThisImport)
//      val updatedSchema = schemaPublisher.submitSchemas(versionedSource).getOrHandle { throw it }
//      val vyneTypes = sourcesFromThisImport.map { (type, _) -> updatedSchema.type(type) }
//      return vyneTypes
//   }
//
//   private fun validate(taxi: String, importRequestSourceName: String): Pair<List<CompilationError>, TaxiDocument> {
//      val importSources = schemaStore.schemaSet().taxiSchemas
//         .map { it.document }
//
//      // First we pre-validate with the compiler.
//      // We need to do this, as we want to ensure the content is valid before writing to disk as VersionedSources.
//      val (messages, compiled) = Compiler(taxi, sourceName = importRequestSourceName, importSources = importSources)
//         .compileWithMessages()
//      if (messages.errors().isNotEmpty()) {
//         throw BadRequestException(messages.errors().joinToString("\n") { it.detailMessage })
//      }
//      return messages to compiled
//   }
//
//   private fun persistSources(typesAndSources: List<Pair<lang.taxi.types.Type, List<CompilationUnit>>>): List<VersionedSource> {
//      // We have to work out a Type-to-file strategy.
//      // As a first pass, I'm using a seperate file for each type.
//      // It's a little verbose on the file system, but it's a reasonable start, as it makes managing edits easier, since
//      // we don't have to worry about insertions / modification within the middle of a file.
//      val versionedSources = typesAndSources.map { (type, compilationUnits) ->
//         val source = reconstructSource(type, compilationUnits)
//         VersionedSource(
//            type.qualifiedName.replace(".", "/") + ".taxi",
//            VersionedSource.DEFAULT_VERSION.toString(),
//            source)
//      }
//      return schemaRepository.writeSources(versionedSources)
//   }
//
//   private fun reconstructSource(type: lang.taxi.types.Type, compilationUnits: List<CompilationUnit>): String {
//      val imports = if (type is ObjectType) {
//         type.referencedTypes
//            .map { referencedType -> referencedType.formattedInstanceOfType ?: referencedType }
//            .map { "import ${it.qualifiedName}" }
//            .distinct()
//      } else emptyList()
//      val namespace = if (type.toQualifiedName().namespace.isNullOrEmpty()) {
//         ""
//      } else {
//         "namespace ${type.toQualifiedName().namespace}\n"
//      }
//      return """${imports.joinToString("\n")}
//$namespace
//${compilationUnits.joinToString("\n") { it.source.content } }
//""".trim()
//   }
//}
