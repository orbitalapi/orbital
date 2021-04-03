package io.vyne.queryService

import app.cash.turbine.test
import io.vyne.query.QueryResponse
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

class QueryMetaDataServiceTest {

   @ExperimentalTime
   @Test
   fun `observe query meta data events`() = runBlockingTest {

      val queryMetaDataService = QueryMetaDataService()

      //given - queryId and a handle to the metadata shared flow
      val queryId: String = UUID.randomUUID().toString()
      val eventFlow = queryMetaDataService.queryMetaDataEvents(queryId)

      //when - many events regarding the query are published
      queryMetaDataService.reportState(queryId, QueryState.STARTING)
      queryMetaDataService.reportState(queryId, QueryState.RUNNING)
      queryMetaDataService.reportState(queryId, QueryState.COMPLETE)

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

      val queryMetaDataService = QueryMetaDataService()

      //given - queryId and a handle to the metadata shared flow
      val queryId: String = UUID.randomUUID().toString()
      val eventFlow = queryMetaDataService.queryMetaDataEvents(queryId)

      //when - many events regarding the query are published
      val count = AtomicInteger(0)
      eventFlow.test {

         repeat(5) {
            queryMetaDataService.reportState(queryId, QueryState.RUNNING)
            expectItem()
            count.incrementAndGet()
         }
         cancelAndIgnoreRemainingEvents()
      }

      //then
      assertEquals(5, count.get())

   }

   @ExperimentalTime
   @Test
   fun `get latest query metadata`() = runBlocking {

      val queryMetaDataService = QueryMetaDataService()

      //given - queryId
      val queryId: String = UUID.randomUUID().toString()

      //when - many events regarding the query are published
      queryMetaDataService.reportState(queryId, QueryState.STARTING)
      queryMetaDataService.reportState(queryId, QueryState.RUNNING)
      queryMetaDataService.reportState(queryId, QueryState.COMPLETE)

      //
      val latestMetaData = queryMetaDataService.queryMetaData(queryId)
      assertEquals(QueryState.COMPLETE, latestMetaData.state)
   }

   fun monitored(queryId: String, block: () -> String):String {
      println("Monitoring")
      val ret = block.invoke()
      println("Finished Monitoring")
      return ret
   }

   @Test
   fun `annotation Test` () {
      val anyString = getAddress("1234")
      println("String = $anyString")
   }

   fun getAddress(astring:String):String = monitored(astring) {
      println("Getting the address now ")
      astring
   }

   //{
   //   return astring
  // }

      //String = monitored(astring) {
      //return astring
  // }

}

