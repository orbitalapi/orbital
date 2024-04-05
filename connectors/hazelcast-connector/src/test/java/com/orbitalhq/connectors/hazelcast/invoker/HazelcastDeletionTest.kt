package com.orbitalhq.connectors.hazelcast.invoker

import com.orbitalhq.Vyne
import com.orbitalhq.firstRawObject
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test

class HazelcastDeletionTest : BaseHazelcastInvokerTest() {

   val schema = """
      import com.orbitalhq.hazelcast.HazelcastService
      import com.orbitalhq.hazelcast.HazelcastMap
      import com.orbitalhq.hazelcast.UpsertOperation
      import com.orbitalhq.hazelcast.DeleteOperation
      import com.orbitalhq.hazelcast.CompactObject

      type FilmId inherits Int
      type Title inherits String

      @CompactObject
      @HazelcastMap(name = "films")
      closed parameter model Film {
         @Id
         filmId : FilmId
         title : Title
      }
      @HazelcastService(connectionName = "notUsed")
      service HazelcastService {
         @UpsertOperation
         write operation upsert(Film):Film

         @DeleteOperation(mapName = "films")
         write operation deleteAll()

         @DeleteOperation(mapName = "films")
         write operation deleteByKey(FilmId):Film

         table films : Film[]
      }

      """.trimIndent()

   @Test
   fun `can delete single item from cache using key`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast(schema)
      setupDefaultItems(vyne)

      hazelcastInstance.getMap<Int, Any>("films").size.shouldBe(3)

      vyne.query("""
         given { filmId : FilmId = 100 }
         call HazelcastService::deleteByKey""".trimIndent())
         .results.toList()

      val map = hazelcastInstance.getMap<Int, Any>("films")
      map.size.shouldBe(2)
      map.containsKey(100).shouldBeFalse()

   }

   @Test
   fun `can delete everything from cache`(): Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast(schema)
      setupDefaultItems(vyne)

      hazelcastInstance.getMap<Int, Any>("films").size.shouldBe(3)

      vyne.query("""call HazelcastService::deleteAll""")
         .results.toList()

      hazelcastInstance.getMap<Int, Any>("films").size.shouldBe(0)

   }

   suspend fun setupDefaultItems(vyne: Vyne) {

      vyne.query(
         """given { film:Film = {
         |  filmId : 100,
         |  title : "Star Wars"
         |} }
         |call HazelcastService::upsert""".trimMargin()
      )
         .firstRawObject()

      vyne.query(
         """given { film:Film = {
         |  filmId : 110,
         |  title : "Empire Strikes Back"
         |} }
         |call HazelcastService::upsert""".trimMargin()
      )
         .firstRawObject()
      vyne.query(
         """given { film:Film = {
         |  filmId : 120,
         |  title : "Return of the Jedi"
         |} }
         |call HazelcastService::upsert""".trimMargin()
      )
         .firstRawObject()
   }
}
