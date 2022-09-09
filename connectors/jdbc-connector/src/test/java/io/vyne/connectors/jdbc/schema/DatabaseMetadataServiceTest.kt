package io.vyne.connectors.jdbc.schema

import com.winterbe.expekt.should
import io.vyne.connectors.ConnectionSucceeded
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcColumn
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcIndex
import io.vyne.connectors.jdbc.JdbcTable
import io.vyne.utils.get
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
   fun `can test connection`() {
      connectionBuilder.testConnection(JdbcDriver.H2.metadata.testQuery).get().should.equal(ConnectionSucceeded)
   }

   @Test
   fun canListTables() {
      val tables = connectionBuilder.listTables()
      //Compared to H2 1.4.x, in 2.0.x, listTables() includes tables from INFORMATION_SCHEMA as well
      val publicTables = tables.filter { it.schemaName == "PUBLIC" }
      publicTables.should.have.size(4)
      val actorTable =  tables.first { it.tableName == "ACTOR" }
      actorTable.should.equal(
         JdbcTable(
            schemaName = "PUBLIC",
            tableName = "ACTOR",
            listOf(JdbcColumn("ACTOR_ID", "INTEGER", 32, 0, false)),
            listOf(JdbcIndex(
               tables.first { it.tableName == "ACTOR" }.indexes.first().name,
               listOf(
                  JdbcColumn(
                     "ACTOR_ID",
                     "INTEGER",
                     32,
                     0,
                     false
                  )
               )
            )))
      )

      val cityTable = tables.first { it.tableName == "CITY" }
      cityTable.should.equal(
         JdbcTable(
            schemaName = "PUBLIC",
            tableName = "CITY",
            listOf(JdbcColumn("CITY_ID", "INTEGER", 32, 0, false)),
            listOf(JdbcIndex(
               tables.first { it.tableName == "CITY" }.indexes.first().name,
               listOf(
                  JdbcColumn(
                     "CITY_ID",
                     "INTEGER",
                     32,
                     0,
                     false
                  )
               )
            )))
      )

      val movieActorsTable =  tables.first { it.tableName == "MOVIE_ACTORS" }
      movieActorsTable.should.equal(
         JdbcTable(
            schemaName = "PUBLIC",
            tableName = "MOVIE_ACTORS",
            emptyList(),
            listOf(JdbcIndex(
               tables.first { it.tableName == "MOVIE_ACTORS" }.indexes.first().name,
               listOf(
                  JdbcColumn(
                     "ACTORS_ACTOR_ID",
                     "INTEGER",
                     32,
                     0,
                     false
                  )
               )
            ),
               JdbcIndex(
                  tables.first { it.tableName == "MOVIE_ACTORS" }.indexes.last().name,
                  listOf(
                     JdbcColumn(
                        "MOVIE_MOVIE_ID",
                        "INTEGER",
                        32,
                        0,
                        false
                     )
                  )
               ))
         )
      )

      val movieTable = tables.first { it.tableName == "MOVIE" }
      movieTable.should.equal(
         JdbcTable(
            schemaName = "PUBLIC",
            tableName = "MOVIE",
            listOf(JdbcColumn("MOVIE_ID", "INTEGER", 32, 0, false)),
            listOf(JdbcIndex(
               tables.first { it.tableName == "MOVIE" }.indexes.first().name,
               listOf(
                  JdbcColumn(
                     "MOVIE_ID",
                     "INTEGER",
                     32,
                     0,
                     false
                  )
               )
            ))
         )
      )
   }

   @Test
   fun canListColumnsOfTable() {
      val columns = connectionBuilder.listColumns("PUBLIC", "MOVIE")
      columns.should.have.size(2)
      columns.should.contain.elements(
         // note - columns are nullable by default in jdbc, even if not in Kotlin
         JdbcColumn("TITLE", "CHARACTER VARYING", 255, 0, true),
         JdbcColumn("MOVIE_ID", "INTEGER", 32, 0, false)
      )
   }
}
