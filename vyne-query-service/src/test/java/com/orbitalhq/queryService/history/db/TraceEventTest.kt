package com.orbitalhq.queryService.history.db

import com.orbitalhq.queryService.BaseQueryServiceTest
import com.orbitalhq.queryService.TestSpringConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Import(TestSpringConfig::class)
@SpringBootTest(
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistResults=true",
      "vyne.telemetry.enabled=false",
      "vyne.analytics.writerMaxBatchSize=1",
      "vyne.analytics.writerMaxDuration=100ms",
   ]
)
class TraceEventTest : BaseQueryServiceTest() {
   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>

   }
}
