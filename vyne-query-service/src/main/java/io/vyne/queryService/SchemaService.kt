package io.vyne.queryService

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.queryService.policies.PolicyDto
import io.vyne.queryService.schemas.SchemaUpdatedNotification
import io.vyne.schemaStore.SchemaSourceProvider
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemaStore.VersionedSourceProvider
import io.vyne.schemas.Operation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import lang.taxi.generators.SourceFormatter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SchemaService(private val schemaProvider: SchemaSourceProvider,
                    private val schemaStore: SchemaStore,
                    private val config: QueryServerConfig) {
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
   fun getType(@PathVariable typeName: String): Type {
      return schemaProvider.schema()
         .type(typeName)
   }

   @GetMapping(path = ["/api/services/{serviceName}"])
   fun getService(@PathVariable("serviceName") serviceName: String): Service {
      return schemaProvider.schema()
         .service(serviceName)
   }

   @GetMapping(path = ["/api/services/{serviceName}/{operationName}"])
   fun getOperation(@PathVariable("serviceName") serviceName: String,
                    @PathVariable("operationName") operationName:String): Operation {
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
      @RequestParam("includePrimitives", required = false) includePrimitives: Boolean = false): Schema {

      val result = schemaProvider.schema(memberNames, includePrimitives)
      return result;
   }

   @GetMapping(path = ["/api/schema"], params = ["members", "includeTaxi"])
   fun getTaxi(
      @RequestParam("members") memberNames: List<String>,
      @RequestParam("includePrimitives", required = false) includePrimitives: Boolean = false): SchemaWithTaxi {

      val schema = getTypes(memberNames, includePrimitives)

      val formatter = SourceFormatter(inlineTypeAliases = true)

      val typeSource = formatter.format(schema.types.map { it.sources.joinToString("\n") { it.content } }.joinToString("\n"))
      val operationSource = formatter.format(schema.services.map { it.sourceCode.joinToString("\n") { it.content } }.joinToString("\n"))

      val taxi = typeSource + "\n\n" + operationSource

      return SchemaWithTaxi(schema, taxi)
   }

   // TODO : MP - I don't think this is used
//   @PostMapping(path = ["/schemas"])
//   fun getTypesFromSchema(@RequestBody source: String): Schema {
//      return TaxiSchema.from(source)
//   }

   // TODO : What's the relationship between this and the schema-store-api?
   // SHould probably either align the two api's or remove one.
   // Looks like schema-store-api isn't used anywhere.



}

data class SchemaWithTaxi(val schema: Schema, val taxi: String)
