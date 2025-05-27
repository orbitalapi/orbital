package org.taxilang.playground

import com.orbitalhq.cockpit.core.query.QueryInsightUtils
import com.orbitalhq.playground.StubQueryMessage
import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.query.QueryParseMetadata
import com.orbitalhq.query.QueryProfileData
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.NotFoundException
import com.orbitalhq.utils.Ids
import org.http4k.core.Headers
import org.reactivestreams.Publisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
      } catch (e: Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message!!)
      }
   }

   @GetMapping("/api/query/{queryId}/profile")
   fun getQueryProfile(@PathVariable("queryId") queryId: String): QueryProfileData {
      return stubQueryService.getAndPurgeProfileData(queryId) ?: throw NotFoundException(
         "No profile data found for query $queryId"
      )
   }

   @PostMapping("/api/query")
   fun query(@RequestBody queryMessage: StubQueryMessage): Mono<ResponseEntity<Publisher<Any>>> {
      val queryId: String = Ids.id(prefix = "query-", size = 12)
      val (queryResult, contentType) = stubQueryService.submitQuery(
         queryMessage,
         addDelayToStreams = false,
         queryId = queryId
      )
      val publisher =
         when (queryResult) {
            is Mono<*> -> queryResult.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
            is Flux<*> -> queryResult.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
            else -> error("Unknown type of publisher: ${queryResult::class.simpleName}")
         } as Publisher<Any>
      return Mono.just(
         ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header("x-query-id", queryId)
            .body(publisher)

      )
   }
}
