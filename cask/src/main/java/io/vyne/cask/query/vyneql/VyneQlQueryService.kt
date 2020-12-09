package io.vyne.cask.query.vyneql

import io.vyne.http.HttpHeaders
import io.vyne.utils.log
import io.vyne.vyneql.VyneQLQueryString
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class VyneQlQueryService(private val jdbcTemplate: JdbcTemplate,
                         private val sqlGenerator: VyneQlSqlGenerator) {

   companion object {
      const val REST_ENDPOINT = "/api/vyneQl"
   }

   @PostMapping(REST_ENDPOINT)
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString): ResponseEntity<List<Map<String, Any>>> {
      log().info("Received VyneQl query: $query")
      val statement = sqlGenerator.generateSql(query)
      log().info("Generated sql statement: $statement")
      val result =  if (statement.params.isEmpty()) {
         jdbcTemplate.queryForList(
            statement.sql
         )
      } else {
         jdbcTemplate.queryForList(
            statement.sql,
            *statement.params.toTypedArray()
         )
      }

      return ResponseEntity
         .ok()
         .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
         .body(result)
   }

}
