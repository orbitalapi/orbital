package io.vyne.cask.query.vyneql

import io.vyne.cask.config.CaskQueryDispatcherConfiguration
import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.http.HttpHeaders
import io.vyne.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.types.TaxiQLQueryString
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.Executors

@RestController
class VyneQlQueryService(
   private val jdbcStreamTemplate: JdbcStreamingTemplate,
   private val sqlGenerator: VyneQlSqlGenerator,
   private val caskQueryDispatcherConfiguration: CaskQueryDispatcherConfiguration
) {

   private val vyneQlDispatcher =
      Executors.newFixedThreadPool(caskQueryDispatcherConfiguration.queryDispatcherPoolSize).asCoroutineDispatcher()

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
    * Please not that we're returning Mono<Stream<..>> rather than Mono<List<....>>
    * the reason for that is Jackson Stream Serializer which serializes the response eventually
    * invokes 'close' method on the stream which fires the 'onClose' event handler attached to the
    * Stream in JdbcStreamingTemplate. The Handler in JdbcStreamingTemplate closes the underlying database connection.
    * Without this mechanism we block all DB connections in the pool after several queries.
    * @param query vyneQL submitted as post body
    * @return Stream of results
    */
   @PostMapping(value = [REST_ENDPOINT], produces = [MediaType.APPLICATION_JSON_VALUE])
   suspend fun submitVyneQlQuery(@RequestBody query: TaxiQLQueryString): ResponseEntity<Flow<Map<String, Any>>> {
      log().info("Received VyneQl query: $query")

      val resultsDeferred = resultStreamAsync(query)

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .body(resultsDeferred)
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
   suspend fun submitVyneQlQueryStreamingResponse(@RequestBody query: TaxiQLQueryString): ResponseEntity<Flow<Map<String, Any>>> {
      log().info("Received VyneQl query for streaming response: $query")

      val countResultsDeferred = countResultsAsync(query)
      val resultsDeferred = resultStreamAsync(query)

      val count = countResultsDeferred.await()

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .header(HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT, count.toString())
         .body(resultsDeferred)
   }

   private fun resultStreamAsync(query: TaxiQLQueryString): Flow<Map<String, Any>> {

      val statement = sqlGenerator.generateSql(query)
      log().info("Generated sql statement: $statement")
      return Flux.fromStream {
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
      }.asFlow().flowOn(vyneQlDispatcher)
   }

   private fun countResultsAsync(query: TaxiQLQueryString): Deferred<Int> = CoroutineScope(vyneQlDispatcher).async {
      val statement = sqlGenerator.generateSqlCountRecords(query)
      if (statement.params.isEmpty()) {
         jdbcStreamTemplate.queryForObject(statement.sql, Int::class.java)
      } else {
         jdbcStreamTemplate.queryForObject(statement.sql, Int::class.java, *statement.params.toTypedArray())
      }
   }

}
