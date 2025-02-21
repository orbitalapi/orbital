package com.orbitalhq.cockpit.core.monitoring.operations

import com.orbitalhq.cockpit.core.monitoring.MetricsWindow
import com.orbitalhq.schemas.OperationKind
import com.orbitalhq.schemas.OperationName
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.ZonedDateTime

@Testcontainers
@SpringBootTest(classes = [OperationCountServiceTest.Companion.Config::class])
class OperationCountServiceTest{

   @Autowired
   lateinit var repository: OperationInvocationCountRepository

   lateinit var service: OperationCountService
   lateinit var eventConsumer: QueueingOperationInvocationEventConsumer

   @BeforeEach
   fun setup() {
      eventConsumer = QueueingOperationInvocationEventConsumer()

      service = OperationCountService(repository)
   }

   companion object {

      @SpringBootConfiguration
      @EnableAutoConfiguration
      @EntityScan(basePackageClasses = [OperationInvocationCountRepository::class])
      @EnableJpaRepositories(basePackageClasses = [OperationInvocationCountRepository::class])
      class Config

      @JvmField
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1")
   }

   @Test
   fun `loads operation counts for month to date`() {
      val currentTime = ZonedDateTime.parse("2024-02-20T20:00:00Z")
      repository.saveAll(listOf(
         countedWindow(ZonedDateTime.parse("2024-02-10T20:00:00Z"), 200, "getActors"),
         countedWindow(ZonedDateTime.parse("2024-02-10T20:00:00Z"), 150, "getMovies"),
         countedWindow(ZonedDateTime.parse("2024-02-15T20:00:00Z"), 250, "getActors"),
         countedWindow(ZonedDateTime.parse("2024-02-15T20:00:00Z"), 350, "getMovies"),
         // not part of the current month to date:
         countedWindow(ZonedDateTime.parse("2024-01-15T20:00:00Z"), 350, "getMovies"),
         countedWindow(ZonedDateTime.parse("2024-01-15T20:00:00Z"), 350, "getActors"),
      ))
      val result = service.getOperationCounts(MetricsWindow.MonthToDate, currentTime)
      result["getActors"]!!.count.shouldBe(450)
      result["getMovies"]!!.count.shouldBe(500)
   }


   private fun countedWindow(dateTime: ZonedDateTime, count: Int, operationName: OperationName):OperationInvocationCountWindow {
      return OperationInvocationCountWindow(
         windowStart = dateTime.toInstant(),
         windowEnd = dateTime.toInstant().plusSeconds(300),
         operationName,
         OperationKind.ApiCall,
         count
      )
   }
}
