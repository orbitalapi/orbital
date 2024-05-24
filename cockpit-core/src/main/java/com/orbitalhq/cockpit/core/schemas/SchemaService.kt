package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.VersionedSource
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schema.api.ParsedSourceProvider
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.SavedQuery
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.toVyneSources
import com.orbitalhq.schemas.toVyneQualifiedName
import com.orbitalhq.spring.http.NotFoundException
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
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
   private val schemaProvider: SchemaProvider,
   private val schemaStore: SchemaStore,
   modelFormatSpecs: List<ModelFormatSpec>
) {
   private val formatDetector = FormatDetector(modelFormatSpecs)

   @GetMapping(path = ["/api/schemas/raw"])
   fun listRawSchema(): String {
      return schemaProvider.sourceContent.joinToString("\n")
   }

   @GetMapping("/api/schemas/summary")
   fun getSchemaStateSummary(): SchemaUpdatedNotification {
      val schemaSet = schemaStore.schemaSet
      return SchemaUpdatedNotification(
         schemaSet.id,
         schemaSet.generation,
         schemaSet.sourcesWithErrors.size,
         schemaSet.sourceNamesWithErrors
      )
   }

   @GetMapping(path = ["/api/schemas"])
   fun getVersionedSchemas(): List<VersionedSource> {
      return if (schemaProvider is ParsedSourceProvider) {
         schemaProvider.versionedSources.sortedBy { it.name }
      } else {
         emptyList()
      }
   }

   @GetMapping("/api/schemas/queries")
   fun getSavedQueries(): List<SavedQuery> {
      return schemaProvider.schema.asTaxiSchema().taxi.queries
         .map { query ->
            SavedQuery(
               query.name.toVyneQualifiedName(),
               query.compilationUnits.toVyneSources(),
               SavedQuery.QueryKind.forQueryMode(query.queryMode),
               HttpOperation.fromQuery(query),
               WebsocketOperation.fromQuery(query)
            )
         }
   }

   @GetMapping("/api/schemas/queries/{name}")
   fun getSavedQuery(@PathVariable("name") queryName: String): SavedQuery {
      return getSavedQueries()
         .firstOrNull { it.name.parameterizedName == queryName }
         ?: throw NotFoundException("No query $queryName found")
   }

   @GetMapping(path = ["/api/types/{typeName}"])
   fun getType(@PathVariable typeName: String): ResponseEntity<Type>? {
      val schema = schemaProvider.schema
      return if (schema.hasType(typeName)) {
         ResponseEntity.ok(schema.type(typeName))
      } else {
         ResponseEntity.notFound().build()
      }
   }

   @GetMapping(path = ["/api/services/{serviceName}"])
   fun getService(@PathVariable("serviceName") serviceName: String): Service {
      return schemaProvider.schema
         .service(serviceName)
   }

   @GetMapping(path = ["/api/services/{serviceName}/{operationName}"])
   fun getOperation(
      @PathVariable("serviceName") serviceName: String,
      @PathVariable("operationName") operationName: String
   ): RemoteOperation {
      return schemaProvider.schema
         .service(serviceName)
         .remoteOperation(operationName)
   }

   @GetMapping(path = ["/api/types"])
   fun getTypes(): Schema {
      return schemaProvider.schema
   }


   /**
    * Returns a schema comprised of types, and the types they reference.
    * Optionally, also includes Taxi primitives
    */
   @GetMapping(path = ["/api/schema"], params = ["members"])
   @Deprecated("Is this still called?")
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

   @GetMapping("/api/schema/tree")
   fun getSchemaTree(): List<SchemaTreeNode> {
      val schema = schemaProvider.schema
      return SchemaTreeUtils.getRootNodes(schema)
   }

   @GetMapping("/api/schema/tree", params = ["node"])
   fun getSchemaTree(@RequestParam("node") parentQualifiedName: String): List<SchemaTreeNode> {
      val schema = schemaProvider.schema
      val parentFqn = parentQualifiedName.fqn()
      return SchemaTreeUtils.getChildNodes(parentFqn, schema)
   }

   @GetMapping(path = ["/api/schema/annotations"])
   fun listAllAnnotations(): Mono<List<QualifiedName>> {
      val schema = this.schemaProvider.schema
      return Mono.just(schema.metadataTypes + schema.dynamicMetadata)
   }

   @GetMapping(path = ["/api/types/{typeName}/modelFormats"])
   fun getModelFormatSpecs(@PathVariable("typeName") typeName: String): Set<QualifiedName> {
      val schema = schemaProvider.schema
      if (!schema.hasType(typeName)) {
         throw NotFoundException("Type $typeName was not found in this schema")
      }
      val type = schema.type(typeName)
      return formatDetector.getFormatTypes(type)
   }
}

data class SchemaWithTaxi(val schema: Schema, val taxi: String)
