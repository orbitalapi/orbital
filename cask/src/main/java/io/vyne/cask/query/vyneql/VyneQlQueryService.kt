package io.vyne.cask.query.vyneql

import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.http.HttpHeaders
import io.vyne.utils.log
import kotlinx.coroutines.*
import lang.taxi.types.TaxiQLQueryString
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.stream.Stream
import kotlin.streams.toList

@RestController
class VyneQlQueryService(private val jdbcStreamTemplate: JdbcStreamingTemplate,
                         private val sqlGenerator: VyneQlSqlGenerator) {

   companion object {
      const val REST_ENDPOINT = "/api/vyneQl"
   }

   /**
    * <p> REST endpoint to accept submitted VyneQL at /api/vyneQl and return query results
    *    Header: Accept: application/json
    *    Header: Content-Type: application/json
    *
    *    Produces: application/json
    * </p>
    *
    * @param query vyneQL submitted as post body
    * @return List of results
    */
   @PostMapping(value = [REST_ENDPOINT], produces = [MediaType.APPLICATION_JSON_VALUE])
   suspend fun submitVyneQlQuery(@RequestBody query: TaxiQLQueryString): ResponseEntity<Mono<List<Map<String, Any>>>> {
      log().info("Received VyneQl query: $query")

      val resultsDeferred = resultStreamAsync(query)
      val results = resultsDeferred.await().toList()

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .body( results.toMono() )
   }

   /**
    * <p> REST endpoint to accept submitted VyneQL at /api/vyneQl and return streamed query results
    *    Header: Accept: application/json
    *    Header: Content-Type: application/json
    *
    *    Produces: text/event-stream
    * </p>
    *
    * @param query vyneQL submitted as post body
    * @return Flux of results
    */
   @PostMapping(value = [REST_ENDPOINT], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
   suspend fun submitVyneQlQueryStreamingResponse(@RequestBody query: TaxiQLQueryString): ResponseEntity<Flux<Map<String, Any>>> {
      log().info("Received VyneQl query for streaming response: $query")

      val countResultsDeferred = countResultsAsync(query)
      val resultsDeferred = resultStreamAsync(query)

      val count = countResultsDeferred.await()
      val results = resultsDeferred.await()

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .header(HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT, count.toString())
         .body( results.toFlux() )
   }

   private fun resultStreamAsync(query: TaxiQLQueryString): Deferred<Stream<Map<String, Any>>> = GlobalScope.async {
      val statement = sqlGenerator.generateSql(query)
      log().info("Generated sql statement: $statement")
      if (statement.params.isEmpty()) {
         jdbcStreamTemplate.queryForStream(
            statement.sql,
            ColumnMapRowMapper()
         )
      } else {
         jdbcStreamTemplate.queryForStream(
            statement.sql,
            ColumnMapRowMapper(),
            *statement.params.toTypedArray()
         )
      }
   }

   private fun countResultsAsync(query: TaxiQLQueryString): Deferred<Int> = GlobalScope.async {
      val statement = sqlGenerator.generateSqlCountRecords(query)
      if (statement.params.isEmpty()) {
         jdbcStreamTemplate.queryForObject(statement.sql, Int::class.java)
      } else {
         jdbcStreamTemplate.queryForObject(statement.sql, Int::class.java, *statement.params.toTypedArray())
      }
   }

}
