package io.vyne.cask.query.vyneql

import io.vyne.vyneql.VyneQLQueryString
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.PathVariable
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
   fun submitVyneQlQuery(@RequestBody query: VyneQLQueryString): List<Map<String, Any>> {
      val statement = sqlGenerator.generateSql(query)
      return if (statement.params.isEmpty()) {
         jdbcTemplate.queryForList(
            statement.sql
         )
      } else {
         jdbcTemplate.queryForList(
            statement.sql,
            *statement.params.toTypedArray()
         )
      }
   }

}
