package io.vyne.schema.publisher.http

import io.vyne.SourcePackage
import io.vyne.schema.publisher.SourceSubmissionResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

// Please note that feign client name is specified through vyne.taxonomyServer.name
// We can use vyne.schema-server.name here as there is another @ReactiveFeignClient - SchemaEditorApi
// which uses vyne.schema-server.name configuration option. Due to a bug in playtika, qualifier is not considered properly
// to distinguish these definitions and hence we need to use a different configuration setting here.
@ReactiveFeignClient("\${vyne.taxonomySubmissionServer.name:schema-server}", qualifier = "taxonomySubmissionServer")
interface HttpSchemaSubmitter {
   @PostMapping("/api/schemas/taxi", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
   fun submitSources(@RequestBody submission: SourcePackage): Mono<SourceSubmissionResponse>
}
