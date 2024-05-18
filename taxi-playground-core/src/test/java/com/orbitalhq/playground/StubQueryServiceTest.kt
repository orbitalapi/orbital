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
      val result = queryService.submitQuery(query)
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
         .asA<Flux<Any>>()
         .collectList()
         .block()!!
      result.shouldBe(listOf(mapOf("name" to "Mark")))
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
