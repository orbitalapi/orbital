package com.orbitalhq.cockpit.core.monitoring.operations

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataJpaTest
@ExtendWith(SpringExtension::class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [BaseOperationInvocationCountTest.Companion.Config::class])
abstract class BaseOperationInvocationCountTest {
   @Autowired
   lateinit var repository: OperationInvocationCountRepository

   lateinit var writer: OperationInvocationCountWriter
   lateinit var eventConsumer: QueueingOperationInvocationEventConsumer

   companion object {

      @SpringBootConfiguration
      @EntityScan(basePackageClasses = [OperationInvocationCountRepository::class])
      @EnableJpaRepositories(basePackageClasses = [OperationInvocationCountRepository::class])
      class Config

      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1")
   }

   @BeforeEach()
   fun setup() {
      eventConsumer = QueueingOperationInvocationEventConsumer()
      writer = OperationInvocationCountWriter(
         eventConsumer, repository
      )
   }
}
