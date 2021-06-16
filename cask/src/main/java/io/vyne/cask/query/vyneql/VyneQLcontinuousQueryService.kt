package io.vyne.cask.query.vyneql

import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.cask.services.QueryMonitor
import io.vyne.http.HttpHeaders
import kotlinx.coroutines.reactor.asFlux
import lang.taxi.types.TaxiQLQueryString
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import java.time.Duration
import java.util.stream.Stream

private val logger = KotlinLogging.logger {}

/**
 * Class VyneQLContinuousQueryService
 *
 * Handle queries to /api/continuous/vyneQl accepting TaxiQL queries
 * returning a continuous stream of results as server side events - sse channel is left open
 * any modifications to casks related to the query result in a new emission on the sse channel
 *
 */
@RestController
class VyneQLContinuousQueryService(private val jdbcStreamTemplate: JdbcStreamingTemplate,
                            private val sqlGenerator: VyneQlSqlGenerator, private val queryMonitor: QueryMonitor
) {

   companion object {
      const val REST_CONTINUOUS_QUERY  = "/api/continuous/vyneQl"
   }

   @PostMapping(value = [REST_CONTINUOUS_QUERY], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
   suspend fun submitVyneQlContinousQuery(@RequestBody query: TaxiQLQueryString): ResponseEntity<Flux<Map<String, Any>>> {

      val resultsDeferred = resultStreamAsync( sqlGenerator.generateSql(query))
      var primaryKeyColumn = ""
      val meta = jdbcStreamTemplate.dataSource.connection.metaData
      val primaryKeyResults = meta.getPrimaryKeys(null, null, sqlGenerator.toCaskTableName(query))

      while (primaryKeyResults.next()) {
         val columnName: String = primaryKeyResults.getString("COLUMN_NAME")
         primaryKeyColumn=columnName
      }

      val initialResultsFlow = resultsDeferred.toFlux()

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .body( initialResultsFlow
            .concatWith(
               queryMonitor
                  .registerCaskMonitor(sqlGenerator.toCaskTableName(query))
                  .asFlux()
                  .bufferTimeout(50, Duration.ofMillis(2000))
                  .filter {it.isNotEmpty()}
                  .concatMap {
                     val join = it.map { "'${it[primaryKeyColumn]}'" }.joinToString(",")
                     val filter = "\"$primaryKeyColumn\" in ( $join )"
                     val querySql = sqlGenerator.generateSql(query, filter)
                     resultStreamAsync(querySql).toFlux()
                  }
            )
         )
   }

   fun resultStreamAsync(statement: SqlStatement): Stream<Map<String, Any>>  {

      logger.debug {"Generated sql statement: $statement" }
      if (statement.params.isEmpty()) {
         return jdbcStreamTemplate.queryForStream(
            statement.sql,
            ColumnMapRowMapper()
         )
      } else {
         return jdbcStreamTemplate.queryForStream(
            statement.sql,
            ColumnMapRowMapper(),
            *statement.params.toTypedArray()
         )
      }
   }

}
