package io.vyne.cask.query.vyneql

import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.http.HttpHeaders
import io.vyne.utils.log
import io.vyne.vyneql.VyneQLQueryString
import kotlinx.coroutines.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
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
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString): ResponseEntity<List<Map<String, Any>>> {
      log().info("Received VyneQl query: $query")

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .body( resultStream(query).toList() )
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
   suspend fun submitVyneQlQueryStreamingResponse(@RequestBody query: VyneQLQueryString): ResponseEntity<Flux<Map<String, Any>>> {
      log().info("Received VyneQl query for streaming response: $query")

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .header(HttpHeaders.STREAM_RECORD_COUNT, withContext(Dispatchers.Default) { countResults(query) }.toString())
         .body( resultStream(query).toFlux() )
   }

   private fun resultStream(query: VyneQLQueryString): Stream<Map<String, Any>> {
      val statement = sqlGenerator.generateSql(query)
      log().info("Generated sql statement: $statement")
      return if (statement.params.isEmpty()) {
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

   private suspend fun countResults(query: VyneQLQueryString): Int {
      val statement = sqlGenerator.generateSqlCountRecords(query)
      return if (statement.params.isEmpty()) {
         jdbcStreamTemplate.queryForObject(statement.sql, Int::class.java)
      } else {
         jdbcStreamTemplate.queryForObject(statement.sql, Int::class.java, *statement.params.toTypedArray())
      }
   }

}
