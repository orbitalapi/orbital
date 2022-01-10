package io.vyne.connectors.jdbc.schema

import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcColumn
import io.vyne.connectors.jdbc.JdbcTable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = [JdbcTaxiSchemaGeneratorTestConfig::class])
@RunWith(SpringRunner::class)
class DatabaseMetadataServiceTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   lateinit var connectionBuilder: DatabaseMetadataService

   @Before
   fun setup() {
      connectionBuilder = DatabaseMetadataService(jdbcTemplate)
   }

   @Test
   fun canListTables() {
      val tables = connectionBuilder.listTables()
      tables.should.have.size(4)
      tables.should.contain.elements(
         JdbcTable(schemaName = "PUBLIC", tableName = "ACTOR"),
         JdbcTable(schemaName = "PUBLIC", tableName = "CITY"),
         JdbcTable(schemaName = "PUBLIC", tableName = "MOVIE_ACTORS"),
         JdbcTable(schemaName = "PUBLIC", tableName = "MOVIE"),
      )
   }

   @Test
   fun canListColumnsOfTable() {
      val columns = connectionBuilder.listColumns("PUBLIC", "MOVIE")
      columns.should.have.size(2)
      columns.should.contain.elements(
         // note - columns are nullable by default in jdbc, even if not in Kotlin
         JdbcColumn("TITLE", "VARCHAR", 255, 0, true),
         JdbcColumn("MOVIE_ID", "INTEGER", 10, 0, false)
      )
   }
}
