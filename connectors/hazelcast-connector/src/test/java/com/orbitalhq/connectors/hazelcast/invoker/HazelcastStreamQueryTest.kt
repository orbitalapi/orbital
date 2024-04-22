package com.orbitalhq.connectors.hazelcast.invoker

import app.cash.turbine.test
import com.orbitalhq.Vyne
import com.orbitalhq.expectTypedObject
import com.orbitalhq.firstRawObject
import io.kotest.common.runBlocking
import kotlinx.coroutines.delay
import lang.taxi.utils.log
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class HazelcastStreamQueryTest : BaseHazelcastInvokerTest() {

   val valuesToInsert = listOf(
      """
         {
             filmId : 100,
             title : "Star Wars",
             languages : ["English" , "American" ],
             director : { name : "George" },
             cast : [ {name : "Mark" }, {name: "Carrie" } ]
         }
         """,
      """
         {
             filmId : 200,
             title : "Empire Strikes Back",
             languages : ["English" , "American" ],
             director : { name : "George" },
             cast : [ {name : "Mark" }, {name: "Carrie" } ]
         }
         """,
      """
         {
             filmId : 300,
             title : "Return of the Jedi",
             languages : ["English" , "American" ],
             director : { name : "George" },
             cast : [ {name : "Mark" }, {name: "Carrie" } ]
         }
         """
   )

   @Test
   fun `can query a stream from Hazelcast`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      vyne.query("""stream { Film }""")
         .results
         .test(timeout = Duration.parse("5s")) {
            write(vyne, valuesToInsert[0])
            expectTypedObject()
            write(vyne, valuesToInsert[1])
            expectTypedObject()
            write(vyne, valuesToInsert[2])
            expectTypedObject()
            cancel()
         }
   }

   @Test
   @Disabled("not implemented - requires enhancement of core orbital to support stream-with-parameters")
   fun `can query a stream using criteria from Hazelcast`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      vyne.query("""stream { Film( FilmId < 300 ) }""")
         .results
         .test(timeout = Duration.parse("50s")) {
            write(vyne, valuesToInsert[0])
            expectTypedObject()
            write(vyne, valuesToInsert[1])
            expectTypedObject()
            write(vyne, valuesToInsert[2])
            expectTypedObject()
            cancel()
         }
   }

   @Test
   fun `can query a stream using filter function from Hazelcast`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      vyne.query("""stream { Film.filterEach( (FilmId) -> FilmId < 300  ) }""")
         .results
         .test(timeout = Duration.parse("10s")) {
            write(vyne, valuesToInsert[0])
            expectTypedObject()
            write(vyne, valuesToInsert[1])
            expectTypedObject()
            write(vyne, valuesToInsert[2])
            cancel()
         }
   }

   private fun write(vyne: Vyne, valueToInsert: String) {
      runBlocking {
         val result = vyne.query(
            """
         given { film: Film = $valueToInsert }
         call HazelcastService::upsert
      """.trimIndent()
         )
            .firstRawObject()
         log().info("Wrote 1 record")
      }

   }
}
