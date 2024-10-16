package com.orbitalhq.queryService

import app.cash.turbine.test
import com.hazelcast.client.test.TestHazelcastFactory
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.orbitalhq.query.CancelRequestHandler
import com.orbitalhq.query.QueryContextEventHandler
import com.orbitalhq.query.QueryResponse
import com.winterbe.expekt.should
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryMonitor
import com.orbitalhq.query.runtime.core.monitor.RunningQueryStatus
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.assertions.timing.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class ActiveQueryMonitorTest {

   val query = TaxiSchema.empty()
      .parseQuery("find { hello: 1 + 2 }").first

   @Test
   fun `observe query meta data events`() = runBlocking {

      val queryId: String = UUID.randomUUID().toString()
      val hazelcast = TestHazelcastFactory().newHazelcastInstance()
      val queryMetaDataService = ActiveQueryMonitor(hazelcast)
      queryMetaDataService.reportStart(queryId, null, query)

      //given - queryId and a handle to the metadata shared flow

      val eventFlow = queryMetaDataService.queryStatusUpdates(queryId)

      //when - many events regarding the query are published

      queryMetaDataService.incrementEmittedRecordCount(queryId)
      queryMetaDataService.incrementEmittedRecordCount(queryId)


   }

   @ExperimentalTime
   @Test
   fun `observe latest query meta data events and all subsequent once subscribed`() = runBlocking {

      val hazelcast = TestHazelcastFactory().newHazelcastInstance()
      val queryMetaDataService = ActiveQueryMonitor(hazelcast)

      //given - queryId and a handle to the metadata shared flow
      val queryId: String = UUID.randomUUID().toString()
      queryMetaDataService.reportStart(queryId, null, query)
      val eventFlow = queryMetaDataService.queryStatusUpdates(queryId)

      //when - many events regarding the query are published
      val count = AtomicInteger(0)
      eventFlow.test {
         repeat(5) {
            queryMetaDataService.incrementEmittedRecordCount(queryId)
            awaitItem()
            count.incrementAndGet()
         }
         cancelAndIgnoreRemainingEvents()
      }

      //then
      assertEquals(5, count.get())

   }

   @Test
   fun `get latest query metadata`(): Unit = runBlocking {

      val hazelcast = TestHazelcastFactory().newHazelcastInstance()
      val queryMetaDataService = ActiveQueryMonitor(hazelcast)

      //given - queryId
      val queryId: String = UUID.randomUUID().toString()

      //when - many events regarding the query are published
      queryMetaDataService.reportStart(queryId, null, query)
      queryMetaDataService.incrementEmittedRecordCount(queryId)
      queryMetaDataService.incrementEmittedRecordCount(queryId)
      queryMetaDataService.incrementEmittedRecordCount(queryId)

      //
      val latestMetaData = queryMetaDataService.queryMetaData(queryId)
      latestMetaData!!.completedProjections.should.equal(3)
   }

   @Test
   fun `can start and stop a query`() :Unit = runBlocking {
      val hazelcast = TestHazelcastFactory().newHazelcastInstance()
      val queryMonitor = ActiveQueryMonitor(hazelcast)

      //given - queryId
      val queryId: String = UUID.randomUUID().toString()
      val clientQueryId: String = UUID.randomUUID().toString()
      val events = mutableListOf<RunningQueryStatus>()

      thread {
         runBlocking {
            queryMonitor.allQueryStatusUpdates().collect {
               events.add(it)
            }
         }
      }


      // Need to set up the event dispatcher, or there's nothing to cancel later
      val queryEventHandler: CancelRequestHandler = mock {  }
      queryMonitor.eventDispatcherForQuery(queryId, listOf(queryEventHandler))
      queryMonitor.reportStart(queryId, clientQueryId, query)
      eventually(5.seconds) {
         events.shouldHaveSize(1)
      }
      events.single().state.shouldBe(QueryResponse.ResponseStatus.RUNNING)
      events.clear()
      queryMonitor.cancelQuery(queryId)
      eventually(5.seconds) {
         // The event handler shold've received the request to cancel.
         verify(queryEventHandler).requestCancel()
      }

      queryMonitor.reportComplete(queryId)
      eventually(5.seconds) {
         events.shouldHaveSize(1)
      }
      events.single().state.shouldBe(QueryResponse.ResponseStatus.COMPLETED)
   }

}

