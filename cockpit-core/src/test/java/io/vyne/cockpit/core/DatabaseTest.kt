package io.vyne.cockpit.core

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class DatabaseTest {
   companion object {
      @Container
      private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1")
         .apply { this.start() }

      @JvmStatic
      @DynamicPropertySource
      fun registerDynamicProperties(registry: DynamicPropertyRegistry) {

         postgreSQLContainer.waitingFor(Wait.forListeningPort())

         registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl)
         registry.add("spring.datasource.username", postgreSQLContainer::getUsername)
         registry.add("spring.datasource.password", postgreSQLContainer::getPassword)
      }
   }
}
