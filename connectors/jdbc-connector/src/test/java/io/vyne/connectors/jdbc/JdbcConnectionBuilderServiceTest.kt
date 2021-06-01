package io.vyne.connectors.jdbc

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
class JdbcConnectionBuilderServiceTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   lateinit var connectionBuilder: JdbcConnectionBuilderService

   @Before
   fun setup() {
      connectionBuilder = JdbcConnectionBuilderService(jdbcTemplate)
   }

   @Test
   fun canListTables() {
      connectionBuilder.listTables()
   }
}
