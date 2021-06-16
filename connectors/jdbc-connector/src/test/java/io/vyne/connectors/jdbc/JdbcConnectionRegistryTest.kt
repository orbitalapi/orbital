package io.vyne.connectors.jdbc

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
class JdbcConnectionRegistryTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   @Test
   fun `can build connection and connect to db`() {
      // TODO : Migrate this to testcontainers
      val connectionDetails = JdbcConnectionConfiguration(
         "pagila",
         JdbcDriver.POSTGRES,
         mapOf(
            "host" to "localhost",
            "database" to "pagila",
            "user" to "vyneuser",
            "password" to "password"
         )
      )
      val template = JdbcUrlConnectionProvider(connectionDetails)
         .build()
      val metaservice = DatabaseMetadataService(template.jdbcTemplate)
      val tables = metaservice.listTables()
      TODO()
   }
}
