package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.stubbing.StubService
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class StreamFilteringTest {

   lateinit var vyne: Vyne;
   lateinit var stub: StubService

   @Before
   fun setup() {
      val (vyne, stub) = testVyne(
         """
         closed model UserUpdateMessage {
            userId : UserId inherits String
            message : StatusMessage inherits String
         }
         type UnrelatedThing inherits String
         service UserService {
            operation getUpdates():Stream<UserUpdateMessage>
         }
      """.trimIndent()
      )

      this.vyne = vyne
      this.stub = stub
   }

   private val messageA = mapOf("userId" to "aaa", "message" to "Fighting a dragon")
   private val messageB = mapOf("userId" to "bbb", "message" to "Stretching")

   @Test
   fun `unfiltered stream returns expected`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldContainAll(messageA, messageB)
   }

   @Test
   fun `can filter a stream using a type expression that projects to a sub type`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage.filterEach( (UserUpdateMessage) -> UserId == 'bbb' ) }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldContainExactlyInAnyOrder(messageB)
   }

   @Test
   fun `can filter a stream using a type expression`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage.filterEach( (UserUpdateMessage) -> UserId == 'bbb' ) }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldContainExactlyInAnyOrder(messageB)
   }

   @Test
   fun `can filter a stream using a type member expression`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage.filterEach( (UserUpdateMessage) -> UserUpdateMessage::UserId == 'bbb' ) }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldHaveSize(1)
   }

   @Test
   fun `can filter a stream using an object member expression`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage.filterEach( (u:UserUpdateMessage) -> u.userId == 'bbb' ) }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldHaveSize(1)
      queryResult.shouldContainExactly(messageB)
   }


   @Test
   fun `can filter a stream using an object type expression`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage.filterEach( (UserUpdateMessage) -> UserUpdateMessage::UserId == 'bbb' ) }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldHaveSize(1)
      queryResult.shouldContainExactly(messageB)
   }

   @Test
   fun `can filter a stream using a member type as the expression input`(): Unit = runBlocking {
      setupResponseStream()
      val queryResult = vyne.query(
         """
         stream { UserUpdateMessage.filterEach( (UserId) -> UserId == 'bbb' ) }
      """.trimIndent()
      ).rawObjects()
      queryResult.shouldHaveSize(1)
      queryResult.shouldContainExactly(messageB)
   }

   private fun setupResponseStream() {
      stub.addResponseFlow("getUpdates") { _, _ ->
         listOf(
            """{ "userId" : "aaa", "message" : "Fighting a dragon" }""",
            """{ "userId" : "bbb", "message" : "Stretching" }"""
         ).map { vyne.parseJson("UserUpdateMessage", it) }
            .asFlow()
      }
   }
}
