package com.orbitalhq.schema.publisher.http

import com.orbitalhq.SourcePackage
import com.orbitalhq.schema.publisher.SourceSubmissionResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange
import reactor.core.publisher.Mono

interface HttpSchemaSubmitter {
   @PostExchange("/api/schemas/taxi", accept = [MediaType.APPLICATION_JSON_VALUE], contentType = MediaType.APPLICATION_JSON_VALUE)
   fun submitSources(@RequestBody submission: SourcePackage): Mono<SourceSubmissionResponse>
}
