package org.taxilang.playground

import com.orbitalhq.cockpit.core.query.QueryInsightUtils
import com.orbitalhq.playground.StubQueryMessage
import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.query.QueryParseMetadata
import com.orbitalhq.schemas.taxi.TaxiSchema
import org.reactivestreams.Publisher
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class PlaygroundQueryService(private val stubQueryService: StubQueryService) {

   private val insightUtils = QueryInsightUtils()

   @PostMapping("/api/query/parse")
   fun parseQuery(@RequestBody query: StubQueryMessage): Mono<QueryParseMetadata> {
      if (query.query.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No query was provided")
      return try {
         val schema = TaxiSchema.fromStrings(listOf(query.schema, StubQueryService.builtInTypes))
         insightUtils.parseQuery(query.query, schema)
            .onErrorResume { e ->
               Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, e.message!!))
            }
      } catch (e:Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message!!)
      }
   }

   @PostMapping("/api/query")
   fun query(@RequestBody queryMessage: StubQueryMessage): Publisher<out Any> {
      return when (val result = stubQueryService.submitQuery(queryMessage, addDelayToStreams = false)) {
         is Mono<*> -> result.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
         is Flux<*> -> result.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
         else -> error("Unknown type of publisher: ${result::class.simpleName}")
      }
   }
}
