package io.vyne.queryService

import com.jayway.awaitility.Awaitility.await
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.ExecutableQuery
import io.vyne.RunningQueryStatus
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.junit.Before
import org.junit.Test
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class ExecutingQueryRepositoryTest {
   lateinit var repository: ExecutingQueryRepository

   @Before
   fun setup() {
      this.repository = ExecutingQueryRepository(InMemoryQueryHistory())
   }

   @Test
   fun `submitted query is in list while running, and then removed when execution is completed`() {
      val future = CompletableFuture.supplyAsync(supply(100, mock<QueryResult>()))
      val query = mockQuery(future)
      repository.submit(query)

      repository.list().should.have.size(1)

      val result = future.get()
      await().atMost(500, TimeUnit.MILLISECONDS).until {
         repository.list().isEmpty()
      }
   }

   @Test
   fun `submitting an already completed future is immediately removed`() {
      val future = CompletableFuture.completedFuture(mock<QueryResult>())
      val query = mockQuery(future)
      repository.submit(query)

      await().atMost(500, TimeUnit.MILLISECONDS).until {
         repository.list().isEmpty()
      }
   }

   private fun mockQuery(future: CompletableFuture<QueryResult>, queryId: String = "queryId"): ExecutableQuery {
      val mock = mock<ExecutableQuery>();
      whenever(mock.queryId).thenReturn(queryId)
      whenever(mock.result).thenReturn(future)
      whenever(mock.currentStatus()).thenReturn(
         RunningQueryStatus(
            queryId,
            "findAll {}",
            "com.Foo".fqn(),
            0,
            null,
            Instant.now(),
            running = true,
            state = QueryResponse.ResponseStatus.RUNNING
         )
      )
      whenever(mock.currentStatusStream()).thenReturn(Flux.empty())
      return mock
   }

   private fun <T> supply(delayInMillis: Int, value: T): Supplier<T> {
      return Supplier {
         log().info("Supplier starting delay")
         Thread.sleep(delayInMillis.toLong())
         log().info("Supplier completing")
         value
      }
   }
}
