package org.taxilang.playground

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.cockpit.core.query.QueryInsightUtils
import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.playground.StubQueryMessage
import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.query.QueryParseMetadata
import com.orbitalhq.query.QueryProfileData
import com.orbitalhq.query.runtime.core.gateway.HttpErrorResponse
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.NotFoundException
import com.orbitalhq.utils.Ids
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

data class QueryValidationResult(
   val isValid: Boolean,
   val errors: List<String>,
   val expected: Any? = null,
   val actual: Any? = null
)

@RestController
class PlaygroundQueryService(private val stubQueryService: StubQueryService) {

   private val objectMapper: ObjectMapper = jacksonObjectMapper()
   private val insightUtils = QueryInsightUtils()

   @PostMapping("/api/query/parse")
   fun parseQuery(@RequestBody query: StubQueryMessage): Mono<QueryParseMetadata> {
      if (query.query.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No query was provided")
      return try {
         val schema = TaxiSchema.fromStrings(listOf(query.schema, StubQueryService.builtInTypes))
         insightUtils.parseQuery(query.query, schema, generateNewQueryPlan = true, arguments = query.parameters)
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
      val (queryResult, contentType) = try {
         stubQueryService.submitQuery(
            queryMessage,
            addDelayToStreams = false,
            queryId = queryId
         )
      } catch (error: OrbitalQueryException) {
         // Errors caught here are exceptions thrown in the query setup phase
         val (statusCode, errorBody, responseHeaders) = HttpErrorResponse.getErrorCodeAndPayload(error)

         // The error body is generally a JSON string -- but returning that gets json escaped.
         // SO, try to parse it back to a map.
         val errorBodyAsMap = try {
            if (errorBody is String) {
               jacksonObjectMapper().readValue<Any>(errorBody as String)
            } else errorBody
         } catch (e:Exception) {
            errorBody
         }
         return Mono.just(
            ResponseEntity.status(statusCode.value())
               .body(Mono.just(errorBodyAsMap))
         )
      }
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

   @PostMapping("/api/validate")
   fun validate(@RequestBody queryMessage: StubQueryMessage): Mono<QueryValidationResult> {
      if (queryMessage.expectedJson.isNullOrBlank()) {
         throw ResponseStatusException(HttpStatus.BAD_REQUEST, "expectedJson must be provided")
      }
      val expectedJsonString = queryMessage.expectedJson!!
      val expectedJsonObject = parseJson(expectedJsonString)

      val queryId: String = Ids.id(prefix = "query-", size = 12)
      val (queryResult, _) = try {
         stubQueryService.submitQuery(
            queryMessage,
            addDelayToStreams = false,
            queryId = queryId
         )
      } catch (error: OrbitalQueryException) {
         val (_, errorBody, _) = HttpErrorResponse.getErrorCodeAndPayload(error)
         return Mono.just(
            QueryValidationResult(
               isValid = false,
               errors = listOf("Query execution failed: $errorBody"),
               expected = expectedJsonObject,
               actual = null
            )
         )
      }

      val resultFlux = when (queryResult) {
         is Mono<*> -> Flux.from(queryResult)
         is Flux<*> -> queryResult
         else -> error("Unknown type of publisher: ${queryResult::class.simpleName}")
      }

      return resultFlux.collectList().map { results ->
         val actual: Any = if (results.size == 1) results.first()!! else results
         val actualJsonNode = objectMapper.valueToTree<JsonNode>(actual)
         val expectedJsonNode = try {
            objectMapper.readTree(expectedJsonString)
         } catch (e: Exception) {
            return@map QueryValidationResult(
               isValid = false,
               errors = listOf("Failed to parse expectedJson: ${e.message}"),
               expected = queryMessage.expectedJson,
               actual = actual
            )
         }

         if (actualJsonNode == expectedJsonNode) {
            QueryValidationResult(isValid = true, errors = emptyList())
         } else {
            val errors = compareJson("", expectedJsonNode, actualJsonNode)
            QueryValidationResult(
               isValid = false,
               errors = errors,
               expected = expectedJsonObject,
               actual = actual
            )
         }
      }
   }

   private fun compareJson(path: String, expected: JsonNode, actual: JsonNode): List<String> {
      val errors = mutableListOf<String>()
      val currentPath = path.ifEmpty { "$" }

      if (expected.nodeType != actual.nodeType) {
         errors.add("Type mismatch at $currentPath: expected ${expected.nodeType} but got ${actual.nodeType}")
         return errors
      }

      when {
         expected.isObject -> {
            val expectedFields = expected.fieldNames().asSequence().toSet()
            val actualFields = actual.fieldNames().asSequence().toSet()
            for (field in expectedFields - actualFields) {
               errors.add("Missing field at $currentPath.$field")
            }
            for (field in actualFields - expectedFields) {
               errors.add("Unexpected field at $currentPath.$field")
            }
            for (field in expectedFields.intersect(actualFields)) {
               errors.addAll(compareJson("$currentPath.$field", expected[field], actual[field]))
            }
         }
         expected.isArray -> {
            if (expected.size() != actual.size()) {
               errors.add("Array size mismatch at $currentPath: expected ${expected.size()} elements but got ${actual.size()}")
            }
            for (i in 0 until minOf(expected.size(), actual.size())) {
               errors.addAll(compareJson("$currentPath[$i]", expected[i], actual[i]))
            }
         }
         else -> {
            if (expected != actual) {
               errors.add("Value mismatch at $currentPath: expected $expected but got $actual")
            }
         }
      }
      return errors
   }

   private fun parseJson(json: String): Any {
      return try {
         objectMapper.readValue<Any>(json)
      } catch (e: Exception) {
         json
      }
   }
}
