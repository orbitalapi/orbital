package com.orbitalhq.connectors.redis.invoker

import com.orbitalhq.Vyne
import com.orbitalhq.firstRawObject
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class RedisQueryTest : BaseRedisInvokerTest() {

   @AfterEach
   fun cleanupRedis() {
      // Ensure Redis is cleaned between tests
      redisContainer.execInContainer("redis-cli", "FLUSHDB")
   }

   @Test
   fun `can use a taxiql statement to query Redis using an id`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         setupDefaultItems(vyne)
         val matchedFilm = vyne.query("""find { Film( FilmId == 100 ) }""")
            .rawObjects()
         matchedFilm.shouldNotBeNull()
         matchedFilm.shouldHaveSize(1)
         matchedFilm.single()["filmId"].shouldBe(100)
         matchedFilm.single()["title"].shouldBe("Star Wars")
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can do a find all from Redis`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         setupDefaultItems(vyne)
         val matchedFilm = vyne.query("""find { Film[] }""")
            .rawObjects()
         matchedFilm.shouldNotBeNull()
         matchedFilm.shouldHaveSize(3)
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can use a taxiql statement to query Redis using criteria returning a single match`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         setupDefaultItems(vyne)
         val matchedFilm = vyne.query("""find { Film[]( FilmId < 105 ) }""")
            .rawObjects()
         matchedFilm.shouldNotBeNull()
         matchedFilm.shouldHaveSize(1)
         matchedFilm.single().get("title").shouldBe("Star Wars")
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can use a taxiql statement to query Redis using criteria returning multiple matches`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         setupDefaultItems(vyne)
         val matchedFilm = vyne.query("""find { Film[]( FilmId < 115 ) }""")
            .rawObjects()
         matchedFilm.shouldNotBeNull()
         matchedFilm.shouldHaveSize(2)
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can use a taxiql statement to query Redis using compound criteria`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         setupDefaultItems(vyne)
         val matchedFilm = vyne.query("""find { Film[]( FilmId < 125 && Title == "Star Wars" ) }""")
            .rawObjects()
         matchedFilm.shouldNotBeNull()
         matchedFilm.shouldHaveSize(1)
         matchedFilm.single().get("title").shouldBe("Star Wars")
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can query Redis when key does not exist`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         // Don't setup any data
         val matchedFilm = vyne.query("""find { Film( FilmId == 999 ) }""")
            .rawObjects()
         matchedFilm.shouldNotBeNull()
         matchedFilm.shouldHaveSize(0)
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can verify Redis keys are stored with correct pattern`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         setupDefaultItems(vyne)

         // Verify keys exist in Redis with the expected pattern
         val keys = connection.sync().keys("film:*")
         keys.shouldHaveSize(3)
         keys.shouldBe(setOf("film:100", "film:110", "film:120"))

         // Verify we can get the value directly
         val film100 = connection.sync().get("film:100")
         film100.shouldNotBeNull()
         film100.contains("Star Wars").shouldBe(true)
      } finally {
         cleanup(connection)
      }
   }

   private suspend fun setupDefaultItems(vyne: Vyne) {
      vyne.query(
         """given { film:Film = {
         |  filmId : 100,
         |  title : "Star Wars",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call RedisService::upsert""".trimMargin()
      )
         .firstRawObject()

      vyne.query(
         """given { film:Film = {
         |  filmId : 110,
         |  title : "Empire Strikes Back",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call RedisService::upsert""".trimMargin()
      )
         .firstRawObject()

      vyne.query(
         """given { film:Film = {
         |  filmId : 120,
         |  title : "Return of the Jedi",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call RedisService::upsert""".trimMargin()
      )
         .firstRawObject()
   }
}
