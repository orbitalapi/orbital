package com.orbitalhq.playground

import com.orbitalhq.utils.asA
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import java.time.Duration

class StubQueryServiceTest {

   lateinit var queryService: StubQueryService

   @BeforeEach
   fun setup() {
      queryService = StubQueryService(streamDelay = Duration.ofMillis(10))
   }

   @Test
   fun `can submit simple query`() {
      val query = StubQueryMessage("", "find { 1 + 2 }")
      val result = queryService.submitQuery(query, addDelayToStreams = false)
         .first
         .asA<Mono<Any>>()
         .block()!!
      result.shouldBe(3)
   }

   @Test
   fun `can submit query with stub`() {
      val query = StubQueryMessage(
         """
         model Actor {
            name : PersonName inherits String
         }
         service ActorService {
            operation getActors():Actor[]
         }
      """.trimIndent(),
         "find { Actor[] }",
         stubs = listOf(
            OperationStub("getActors", """[ { "name" : "Mark" }]""")
         )
      )
      val result = queryService.submitQuery(query)
         .first
         .asA<Flux<Any>>()
         .collectList()
         .block()!!
      result.shouldBe(listOf(mapOf("name" to "Mark")))
   }

   @Test
   fun `can submit query with stub with args with int inputs`() {
      val query = StubQueryMessage(
         """
         model Actor {
            id : ActorId inherits Int
            name : PersonName inherits String
         }
         service ActorService {
            operation getActor(actorId:ActorId):Actor(...)
         }
      """.trimIndent(),
         "find { Actor(ActorId == 25) }",
         stubs = listOf(
            OperationStub(
               "getActor", "",
               conditionalResponses = listOf(
                  ResponseCondition(
                     // Note: value is passed as a string, because that's how it arrives from JSON.
                     // We need to handle the matching in the stubber
                     listOf(ParameterValue("actorId", "25")),
                     StubbedResponse("""{ "id" : 25, "name" : "Mark" }""")
                  )
               )
            )
         )
      )
      val result = queryService.submitQuery(query)
         .first
         .asA<Mono<Any>>()
         .block()!!
      result.shouldBe(mapOf("name" to "Mark", "id" to 25))
   }

   @Test
   fun `can submit query with stub with args with string inputs`() {
      val query = StubQueryMessage(
         """
         model Actor {
            id : ActorId inherits String
            name : PersonName inherits String
         }
         service ActorService {
            operation getActor(actorId:ActorId):Actor(...)
         }
      """.trimIndent(),
         """find { Actor(ActorId == "act-1") }""",
         stubs = listOf(
            OperationStub(
               "getActor", "",
               conditionalResponses = listOf(
                  ResponseCondition(
                     listOf(ParameterValue("actorId", "act-1")),
                     StubbedResponse("""{ "id" : "act-1", "name" : "Mark" }""")
                  )
               )
            )
         )
      )
      val result = queryService.submitQuery(query)
         .first
         .asA<Mono<Any>>()
         .block()!!
      result.shouldBe(mapOf("name" to "Mark", "id" to "act-1"))
   }

   @Test
   fun `can submit streaming query`() {
      val query = StubQueryMessage(
         """
         model Actor {
            name : PersonName inherits String
         }
         service ActorService {
            operation getActors():Stream<Actor>
         }
      """.trimIndent(),
         "stream { Actor }",
         stubs = listOf(
            OperationStub("getActors", """[ { "name" : "Mark" },{ "name" : "Carrie" },{ "name" : "Harrison" }  ]""")
         )
      )
      queryService.submitQuery(query)
         .first
         .asA<Flux<Any>>()
         .test()
         .expectSubscription()
         .expectNext(mapOf("name" to "Mark"))
         .expectNext(mapOf("name" to "Carrie"))
         .expectNext(mapOf("name" to "Harrison"))
         .expectComplete()
         .verify()
   }
}
