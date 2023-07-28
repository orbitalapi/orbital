package io.vyne.query.runtime.core.gateway

import io.vyne.schema.api.SchemaProvider
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.taxilang.openapi.OpenApiGenerator

@RestController
class OpenApiSpecService(
   private val schemaProvider: SchemaProvider,
   private val queryRouteService: QueryRouteService
) {

   @GetMapping("/api/q/meta/{queryName}/oas", produces = ["text/yaml"])
   fun getApiSpecForSavedQuery(@PathVariable("queryName") queryName: String): String {
      val routableQuery = queryRouteService.routes.firstOrNull { query ->
         query.query.name.fullyQualifiedName == queryName
      } ?: error("No query matched the provided route")

      val taxiDocument = schemaProvider.schema.taxi

      val openApiGenerator = OpenApiGenerator()
      val generatedSpecs = openApiGenerator
         .generateOpenApiSpec(taxiDocument, listOf(routableQuery.query.name.fullyQualifiedName))
      val yaml = openApiGenerator.generateYaml(generatedSpecs)
      return yaml.joinToString("\n") { it.content }
   }
}
