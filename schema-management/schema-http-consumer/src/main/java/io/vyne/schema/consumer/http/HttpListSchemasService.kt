package io.vyne.schema.consumer.http

import io.vyne.schema.api.SchemaSet
import org.springframework.web.bind.annotation.GetMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.taxonomyProviderServer.name:schema-server}", qualifier = "taxonomyProviderServer")
interface HttpListSchemasService {
   @GetMapping("/api/schemas/taxi")
   fun listSchemas(): Mono<SchemaSet>

}
