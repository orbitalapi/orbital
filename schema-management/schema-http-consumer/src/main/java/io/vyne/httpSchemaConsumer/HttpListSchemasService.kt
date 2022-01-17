package io.vyne.httpSchemaConsumer

import io.vyne.schemaApi.SchemaSet
import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.taxonomyProviderServer.name:schema-server}", qualifier = "taxonomyProviderServer")
interface HttpListSchemasService {
   @GetMapping("/api/schemas/taxi")
   fun listSchemas(): Mono<SchemaSet>

}
