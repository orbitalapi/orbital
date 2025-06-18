package com.orbitalhq.connectors.hazelcast.invoker

import com.orbitalhq.firstRawObject
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.tracing.CacheRequest
import com.orbitalhq.query.tracing.CacheResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class HazelcastTracingTest : BaseHazelcastInvokerTest() {

   @Test
   fun `emits tracing events when querying Hazelcast map and includes request payload`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Film( FilmId == 100 ) }""",
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify request event
      val requestEvent = eventSink.collectedEvents.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<CacheRequest>()
      requestEvent.eventVerb.shouldBe("Get")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("Key = 100")

      // Verify response event
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<CacheResponse>()
      responseEvent.eventVerb.shouldBe("Get response")
      responseMetadata.recordCount.shouldBe(1)

      // Verify actual query result
      result.shouldHaveSize(1)
      result.single()["filmId"].shouldBe(100)
   }

   @Test
   fun `emits tracing events when querying all items from Hazelcast map`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Film[] }""",
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify response event shows correct record count
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<CacheResponse>()
      responseMetadata.recordCount.shouldBe(3)

      result.shouldHaveSize(3)
   }

   @Test
   @Disabled // TODO
   fun `emits tracing events when no items found in Hazelcast map`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Film( FilmId == 999 ) }""",
         eventBroker = eventBroker
      ).rawObjects()

      // Should still have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify response event for no items found
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<CacheResponse>()
      responseMetadata.recordCount.shouldBe(0)

      val responsePayload = responseMetadata.payload()
      responsePayload.shouldNotBeNull()

      result.shouldHaveSize(0)
   }

   @Test
   fun `emits tracing events when doing upsert operations in Hazelcast map`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """given { film:Film = {
            |  filmId : 200,
            |  title : "New Movie",
            |  languages : ["English"],
            |  director : { name : "Director" },
            |  cast : [ {name : "Actor" } ]
            |} }
            |call HazelcastService::upsert""".trimMargin(),
         eventBroker = eventBroker
      ).firstRawObject()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify request event
      val requestEvent = eventSink.collectedEvents.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<CacheRequest>()
      requestEvent.eventVerb.shouldBe("Upsert")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("New Movie")

      // Verify response event
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<CacheResponse>()
      responseEvent.eventVerb.shouldBe("Upsert response")
      responseMetadata.recordCount.shouldBe(1)

      result.shouldNotBeNull()
      result["title"].shouldBe("New Movie")
   }

   @Test
   fun `emits tracing events when querying with criteria returning multiple matches`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Film[]( FilmId < 115 ) }""",
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify request event contains criteria
      val requestEvent = eventSink.collectedEvents.first()
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<CacheRequest>()
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("FilmId < 115")

      // Verify response event shows correct record count
      val responseEvent = eventSink.collectedEvents.last()
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<CacheResponse>()
      responseMetadata.recordCount.shouldBe(2)

      result.shouldHaveSize(2)
   }

   suspend fun setupDefaultItems(vyne: com.orbitalhq.Vyne) {
      vyne.query("""given { film:Film = {
         |  filmId : 100,
         |  title : "Star Wars",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call HazelcastService::upsert""".trimMargin())
         .firstRawObject()

      vyne.query("""given { film:Film = {
         |  filmId : 110,
         |  title : "Empire Strikes Back",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call HazelcastService::upsert""".trimMargin())
         .firstRawObject()

      vyne.query("""given { film:Film = {
         |  filmId : 120,
         |  title : "Return of the Jedi",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call HazelcastService::upsert""".trimMargin())
         .firstRawObject()
   }
}
