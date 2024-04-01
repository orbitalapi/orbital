package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.hazelcast.HazelcastInstanceProvider
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.testVyneWithStub
import com.orbitalhq.typedObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HazelcastQueryTest : BaseHazelcastInvokerTest() {

   @Test
   fun `can use a taxiql statement to query a hazelcast map using an id`() : Unit = runBlocking{
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)
      val matchedFilm = vyne.query("""find { Film( FilmId == 100 ) }""")
         .rawObjects()
      matchedFilm.shouldNotBeNull()
      matchedFilm.shouldHaveSize(1)
      matchedFilm.single()["filmId"].shouldBe(100)
   }

   @Test
   fun `can do a find all from a hazelcast map`():Unit = runBlocking {
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)
      val matchedFilm = vyne.query("""find { Film[] }""")
         .rawObjects()
      matchedFilm.shouldNotBeNull()
      matchedFilm.shouldHaveSize(3)
   }

   @Test
   fun `can use a taxiql statement to query a hazelcast map using criteria returning a single match`() : Unit = runBlocking{
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)
      val matchedFilm = vyne.query("""find { Film[]( FilmId < 105 ) }""")
         .rawObjects()
      matchedFilm.shouldNotBeNull()
      matchedFilm.shouldHaveSize(1)
      matchedFilm.single().get("title").shouldBe("Star Wars")
   }

   @Test
   fun `can use a taxiql statement to query a hazelcast map using criteria returning a multiple matches`() : Unit = runBlocking{
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)
      val matchedFilm = vyne.query("""find { Film[]( FilmId < 115 ) }""")
         .rawObjects()
      matchedFilm.shouldNotBeNull()
      matchedFilm.shouldHaveSize(2)
   }

   @Test
   fun `can use a taxiql statement to query a hazelcast map using compound criteria`() : Unit = runBlocking{
      val (hazelcastInstance, vyne, stub) = vyneWithHazelcast()
      setupDefaultItems(vyne)
      val matchedFilm = vyne.query("""find { Film[]( FilmId < 125 && Title == "Star Wars" ) }""")
         .rawObjects()
      matchedFilm.shouldNotBeNull()
      matchedFilm.shouldHaveSize(1)
      matchedFilm.single().get("title").shouldBe("Star Wars")
   }

   suspend fun setupDefaultItems(vyne:Vyne) {

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


class TestHazelcastProvider(private val instance: HazelcastInstance) : HazelcastInstanceProvider {
   override fun provide(config: HazelcastConfiguration): HazelcastInstance {
      return instance
   }

   override fun hazelcastConnection(connectionName: String?): Pair<HazelcastInstance, HazelcastConfiguration> {
      return instance to mock {  }
   }

   override fun canProvideHazelcastInstance(connectionName: String?): Boolean {
      return true
   }
}
