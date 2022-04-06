package io.vyne.queryService

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.models.format.FormatDetector
import io.vyne.models.format.ModelFormatSpec
import io.vyne.queryService.policies.PolicyDto
import io.vyne.queryService.schemas.SchemaUpdatedNotification
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.api.VersionedSourceProvider
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.Operation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.spring.http.NotFoundException
import lang.taxi.generators.SourceFormatter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

// See also LocalSchemaEditingService, which provides endpoints for modifying types
@RestController
class SchemaService(
   private val schemaProvider: SchemaSourceProvider,
   private val schemaStore: SchemaStore,
   modelFormatSpecs: List<ModelFormatSpec>
) {
   private val formatDetector = FormatDetector(modelFormatSpecs)
   @GetMapping(path = ["/api/schemas/raw"])
   fun listRawSchema(): String {
      return schemaProvider.schemaStrings().joinToString("\n")
   }

   @GetMapping("/api/schemas/summary")
   fun getSchemaStateSummary(): SchemaUpdatedNotification {
      val schemaSet = schemaStore.schemaSet()
      return SchemaUpdatedNotification(
         schemaSet.id,
         schemaSet.generation,
         schemaSet.invalidSources.size
      )
   }

   @GetMapping(path = ["/api/parsedSources"])
   fun getParsedSources(): List<ParsedSource> {
      return if (schemaProvider is VersionedSourceProvider) {
         schemaProvider.parsedSources.sortedBy { it.source.name }
      } else {
         emptyList()
      }
   }

   @GetMapping(path = ["/api/schemas"])
   fun getVersionedSchemas(): List<VersionedSource> {
      return if (schemaProvider is VersionedSourceProvider) {
         schemaProvider.versionedSources.sortedBy { it.name }
      } else {
         emptyList()
      }
   }

   @GetMapping(path = ["/api/types/{typeName}"])
   fun getType(@PathVariable typeName: String): ResponseEntity<Type>? {
      val schema = schemaProvider.schema()
      return if (schema.hasType(typeName)) {
         ResponseEntity.ok(schema.type(typeName))
      } else {
         ResponseEntity.notFound().build()
      }
   }

   @GetMapping(path = ["/api/services/{serviceName}"])
   fun getService(@PathVariable("serviceName") serviceName: String): Service {
      return schemaProvider.schema()
         .service(serviceName)
   }

   @GetMapping(path = ["/api/services/{serviceName}/{operationName}"])
   fun getOperation(
      @PathVariable("serviceName") serviceName: String,
      @PathVariable("operationName") operationName: String
   ): Operation {
      return schemaProvider.schema()
         .service(serviceName)
         .operation(operationName)
   }

   @GetMapping(path = ["/api/types"])
//   @JsonView(TypeLightView::class)
   fun getTypes(): Schema {
      return schemaProvider.schema()
   }


   @GetMapping(path = ["/api/types/{typeName}/policies"])
   fun getPolicies(@PathVariable("typeName") typeName: String): List<PolicyDto> {
      val schema = schemaProvider.schema()
      if (!schema.hasType(typeName)) {
         throw NotFoundException("Type $typeName was not found in this schema")
      }
      val type = schema.type(typeName)
      val policy = schema.policy(type)
      return listOfNotNull(policy).map { PolicyDto.from(it) }
   }

   /**
    * Returns a schema comprised of types, and the types they reference.
    * Optionally, also includes Taxi primitives
    */
   @GetMapping(path = ["/api/schema"], params = ["members"])
   fun getTypes(
      @RequestParam("members") memberNames: List<String>,
      @RequestParam("includePrimitives", required = false) includePrimitives: Boolean = false
   ): Schema {

      val result = schemaProvider.schema(memberNames, includePrimitives)
      return result
   }

   @GetMapping(path = ["/api/schema"], params = ["members", "includeTaxi"])
   fun getTaxi(
      @RequestParam("members") memberNames: List<String>,
      @RequestParam("includePrimitives", required = false) includePrimitives: Boolean = false
   ): SchemaWithTaxi {

      val schema = getTypes(memberNames, includePrimitives)

      val formatter = SourceFormatter(inlineTypeAliases = true)

      val typeSource =
         formatter.format(schema.types.map { it.sources.joinToString("\n") { it.content } }.joinToString("\n"))
      val operationSource =
         formatter.format(schema.services.map { it.sourceCode.joinToString("\n") { it.content } }.joinToString("\n"))

      val taxi = typeSource + "\n\n" + operationSource

      return SchemaWithTaxi(schema, taxi)
   }

   @GetMapping(path = ["/api/schema/annotations"])
   fun listAllAnnotations(): Mono<List<QualifiedName>> {
      val schema = this.schemaProvider.schema()
      return Mono.just(schema.metadataTypes + schema.dynamicMetadata)
   }

   @GetMapping(path = ["/api/types/{typeName}/modelFormats"])
   fun getModelFormatSpecs(@PathVariable("typeName") typeName: String): Set<QualifiedName> {
      val schema = schemaProvider.schema()
      if (!schema.hasType(typeName)) {
         throw NotFoundException("Type $typeName was not found in this schema")
      }
      val type = schema.type(typeName)
      return formatDetector.getFormatTypes(type)
   }
}

data class SchemaWithTaxi(val schema: Schema, val taxi: String)
