package io.vyne.connectors.jdbc

import com.winterbe.expekt.should
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
class JdbcConnectionRegistryTest {

   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   @Test
   fun `can build connection and connect to db`() {
      val h2ConnectionMetadata = jdbcTemplate.dataSource.connection.metaData
      val connectionDetails = JdbcUrlConnection(
         "h2",
         JdbcDriver.H2,
         connectionDetails = JdbcConnectionDetails(h2ConnectionMetadata.url, h2ConnectionMetadata.userName, "")
      )

      val template = JdbcUrlConnectionProvider(connectionDetails)
         .build()
      val metadataService = DatabaseMetadataService(template.jdbcTemplate, connectionDetails.driver)
      metadataService.testConnection().should.be.`true`
      val tables = metadataService.listTables()
      tables.should.have.size(2)
   }
}
