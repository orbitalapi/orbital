package org.taxilang.playground

import com.orbitalhq.playground.StubQueryMessage
import com.orbitalhq.playground.StubQueryService
import lang.taxi.query.TaxiQlQuery
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

   @PostMapping("/api/query/parse")
   fun parseQuery(@RequestBody query: StubQueryMessage): TaxiQlQuery {
      return try {
         stubQueryService.parseQuery(query)
      } catch (e:Exception) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message!!)
      }
   }

   @PostMapping("/api/query")
   fun query(@RequestBody queryMessage: StubQueryMessage): Publisher<out Any> {
      return when (val result = stubQueryService.submitQuery(queryMessage)) {
         is Mono<*> -> result.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
         is Flux<*> -> result.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
         else -> error("Unknown type of publisher: ${result::class.simpleName}")
      }
   }
}
