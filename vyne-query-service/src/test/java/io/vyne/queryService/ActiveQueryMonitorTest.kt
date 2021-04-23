package io.vyne.queryService

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.vyne.query.active.ActiveQueryMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class ActiveQueryMonitorTest {

   @Test
   fun `observe query meta data events`() = runBlockingTest {

      val queryMetaDataService = ActiveQueryMonitor()

      //given - queryId and a handle to the metadata shared flow
      val queryId: String = UUID.randomUUID().toString()
      val eventFlow = queryMetaDataService.queryStatusUpdates(queryId)

      //when - many events regarding the query are published
      queryMetaDataService.reportStart(queryId, null, "")
      queryMetaDataService.incrementEmittedRecordCount(queryId)
      queryMetaDataService.incrementEmittedRecordCount(queryId)

      //then
      eventFlow.test {
         //expect latest item
         expectItem()
         //but not more
         expectNoEvents()
      }

   }

   @ExperimentalTime
   @Test
   fun `observe latest query meta data events and all subsequent once subscribed`() = runBlocking {

      val queryMetaDataService = ActiveQueryMonitor()

      //given - queryId and a handle to the metadata shared flow
      val queryId: String = UUID.randomUUID().toString()
      queryMetaDataService.reportStart(queryId, null, "")
      val eventFlow = queryMetaDataService.queryStatusUpdates(queryId)

      //when - many events regarding the query are published
      val count = AtomicInteger(0)
      eventFlow.test {
         repeat(5) {
            queryMetaDataService.incrementEmittedRecordCount(queryId)
            expectItem()
            count.incrementAndGet()
         }
         cancelAndIgnoreRemainingEvents()
      }

      //then
      assertEquals(5, count.get())

   }

   @Test
   fun `get latest query metadata`():Unit = runBlocking {

      val queryMetaDataService = ActiveQueryMonitor()

      //given - queryId
      val queryId: String = UUID.randomUUID().toString()

      //when - many events regarding the query are published
      queryMetaDataService.reportStart(queryId, null, "")
      queryMetaDataService.incrementEmittedRecordCount(queryId)
      queryMetaDataService.incrementEmittedRecordCount(queryId)
      queryMetaDataService.incrementEmittedRecordCount(queryId)

      //
      val latestMetaData = queryMetaDataService.queryMetaData(queryId)
      latestMetaData!!.completedProjections.should.equal(3)
   }

}

