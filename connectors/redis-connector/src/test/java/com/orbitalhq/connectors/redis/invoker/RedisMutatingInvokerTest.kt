package com.orbitalhq.connectors.redis.invoker

import com.orbitalhq.firstTypedObject
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.RemoteCallOperationResultHandler
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class RedisMutatingInvokerTest : BaseRedisInvokerTest() {

   @AfterEach
   fun cleanupRedis() {
      redisContainer.execInContainer("redis-cli", "FLUSHDB")
   }

   @Test
   fun `can write object to Redis`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         val operationResults = mutableListOf<OperationResult>()
         val remoteCallOperationResultHandler = object : RemoteCallOperationResultHandler {
            override fun recordResult(operation: OperationResult, queryId: String) {
               operationResults.add(operation)
            }
         }
         val queryEventBroker = QueryContextEventBroker(traceSpan = TraceContext.noOp().rootSpan)
         queryEventBroker.addHandler(remoteCallOperationResultHandler)

         val upsertedInstance = vyne.query(
            vyneQlQuery = """given { film:Film = {
         |  filmId : 100,
         |  title : "Star Wars",
         |  languages : ["English" , "American" ],
         |  director : { name : "George" },
         |  cast : [ {name : "Mark" }, {name: "Carrie" } ]
         |} }
         |call RedisService::upsert""".trimMargin(),
            eventBroker = queryEventBroker
         )
            .firstTypedObject()

         val operationResult = operationResults.first()
         operationResult.remoteCall.exchange.shouldBeInstanceOf<CacheExchange>()
         (operationResult.remoteCall.exchange as CacheExchange).cacheType.shouldBe(CacheExchange.CacheType.Redis)

         // Verify the data is actually in Redis
         val storedValue = connection.sync().get("film:100")
         storedValue.shouldNotBeNull()
         storedValue.contains("Star Wars").shouldBe(true)
         storedValue.contains("\"filmId\":100").shouldBe(true)
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can read and write multiple objects`(): Unit = runBlocking {
      val schema = """
      import com.orbitalhq.redis.RedisService
      import com.orbitalhq.redis.RedisKey
      import com.orbitalhq.redis.RedisUpsertOperation
      import com.orbitalhq.redis.RedisDeleteOperation

      type FilmId inherits Int
      type Title inherits String

      closed model ApiFilm {
         @Id
         filmId : FilmId
         title : Title
      }

      service ApiService {
         operation getFilms():ApiFilm[]
      }

      @RedisKey(pattern = "film:{filmId}")
      closed parameter model RedisFilm {
         @Id
         id : FilmId
         name : Title
      }

      @RedisService(connectionName = "test")
      service RedisService {
         @RedisUpsertOperation
         write operation upsert(RedisFilm):RedisFilm

         @RedisDeleteOperation(keyPattern = "film:*")
         write operation deleteAll()

         table films : RedisFilm[]
      }

      """.trimIndent()

      val (connection, vyne, stub) = vyneWithRedis(schema)
      try {
         stub.addResponse(
            "getFilms", vyne.parseJson(
               "ApiFilm[]", """
         [
            { "filmId" : 100, "title" : "Star Wars" },
            { "filmId" : 200, "title" : "Empire Strikes Back" },
            { "filmId" : 300, "title" : "Return of the Jedi" }
         ]
      """.trimIndent()
            )
         )

         val upsertResult = vyne.query(
            """
         find { ApiFilm[] }
         call RedisService::upsert
      """.trimIndent()
         )
            .rawObjects()
         upsertResult.shouldHaveSize(3)

         // Verify all keys exist in Redis
         val keys = connection.sync().keys("film:*")
         keys.shouldHaveSize(3)

         val findResult = vyne.query("""find { RedisFilm[] }""")
            .rawObjects()
         findResult.shouldHaveSize(3)
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can delete by key`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         // Insert some data
         vyne.query(
            """given { film:Film = {
         |  filmId : 100,
         |  title : "Star Wars",
         |  languages : ["English"],
         |  director : { name : "George" },
         |  cast : []
         |} }
         |call RedisService::upsert""".trimMargin()
         ).firstTypedObject()

         // Verify it exists
         connection.sync().get("film:100").shouldNotBeNull()

         // Delete it
         vyne.query("""given { filmId : FilmId = 100 } call RedisService::delete""")
            .firstTypedObject()

         // Verify it's gone
         connection.sync().get("film:100").shouldBeNull()
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can delete all by pattern`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         // Insert multiple films
         vyne.query(
            """given { film:Film = {
         |  filmId : 100, title : "Film 1",
         |  languages : [], director : { name : "Director" }, cast : []
         |} }
         |call RedisService::upsert""".trimMargin()
         ).firstTypedObject()

         vyne.query(
            """given { film:Film = {
         |  filmId : 200, title : "Film 2",
         |  languages : [], director : { name : "Director" }, cast : []
         |} }
         |call RedisService::upsert""".trimMargin()
         ).firstTypedObject()

         vyne.query(
            """given { film:Film = {
         |  filmId : 300, title : "Film 3",
         |  languages : [], director : { name : "Director" }, cast : []
         |} }
         |call RedisService::upsert""".trimMargin()
         ).firstTypedObject()

         // Verify all exist
         connection.sync().keys("film:*").shouldHaveSize(3)

         // Delete all
         vyne.query("""call RedisService::deleteAll""")
            .firstTypedObject()

         // Verify all are gone
         connection.sync().keys("film:*").shouldHaveSize(0)
      } finally {
         cleanup(connection)
      }
   }

   @Test
   fun `can write with TTL`(): Unit = runBlocking {
      val schemaWithTTL = """
      import com.orbitalhq.redis.RedisService
      import com.orbitalhq.redis.RedisKey
      import com.orbitalhq.redis.RedisTTL
      import com.orbitalhq.redis.RedisUpsertOperation

      @RedisKey(pattern = "film:{filmId}")
      @RedisTTL(seconds = 2)
      closed model Film {
         @Id
         filmId : FilmId inherits Int
         title : Title inherits String
      }

      @RedisService(connectionName = "test")
      service RedisService {
         @RedisUpsertOperation
         write operation upsert(Film):Film
      }
      """.trimIndent()

      val (connection, vyne, stub) = vyneWithRedis(schemaWithTTL)
      try {
         vyne.query(
            """given { film:Film = { filmId : 100, title : "Star Wars" } }
         |call RedisService::upsert""".trimMargin()
         ).firstTypedObject()

         // Verify it exists with TTL
         connection.sync().get("film:100").shouldNotBeNull()
         val ttl = connection.sync().ttl("film:100")
         ttl.shouldNotBeNull()
         (ttl > 0).shouldBe(true)
         (ttl <= 2).shouldBe(true)

         // Wait for expiration
         Thread.sleep(2500)

         // Verify it's gone
         connection.sync().get("film:100").shouldBeNull()
      } finally {
         cleanup(connection)
      }
   }
}
