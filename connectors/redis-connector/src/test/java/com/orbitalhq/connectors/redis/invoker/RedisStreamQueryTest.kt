package com.orbitalhq.connectors.redis.invoker

import app.cash.turbine.test
import com.orbitalhq.Vyne
import com.orbitalhq.expectTypedObject
import com.orbitalhq.firstRawObject
import io.kotest.common.runBlocking
import kotlinx.coroutines.delay
import lang.taxi.utils.log
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class RedisStreamQueryTest : BaseRedisInvokerTest() {

   @AfterEach
   fun cleanupRedis() {
      redisContainer.execInContainer("redis-cli", "FLUSHDB")
   }

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
   @Disabled("Streaming tests require keyspace notifications enabled - complex to set up in testcontainers")
   fun `can query a stream from Redis using keyspace notifications`(): Unit = runBlocking {
      val (connection, vyne, stub) = vyneWithRedis()
      try {
         // Note: This would require enabling keyspace notifications in Redis:
         // CONFIG SET notify-keyspace-events KEA
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
      } finally {
         cleanup(connection)
      }
   }

   @Test
   @Disabled("Pub/Sub streaming requires channel setup")
   fun `can query a stream from Redis Pub Sub`(): Unit = runBlocking {
      val pubSubSchema = """
      import com.orbitalhq.redis.RedisService
      import com.orbitalhq.redis.RedisKey
      import com.orbitalhq.redis.RedisPubSubChannel
      import com.orbitalhq.redis.RedisUpsertOperation

      @RedisKey(pattern = "film:{filmId}")
      @RedisPubSubChannel(channel = "films")
      closed model Film {
         @Id
         filmId : FilmId inherits Int
         title : Title inherits String
      }

      @RedisService(connectionName = "test")
      service RedisService {
         @RedisUpsertOperation
         write operation upsert(Film):Film

         stream films : Stream<Film>
      }
      """.trimIndent()

      val (connection, vyne, stub) = vyneWithRedis(pubSubSchema)
      try {
         // Start streaming
         vyne.query("""stream { Film }""")
            .results
            .test(timeout = Duration.parse("5s")) {
               // Publish messages to the channel
               publishToChannel(connection, "films", valuesToInsert[0])
               expectTypedObject()
               publishToChannel(connection, "films", valuesToInsert[1])
               expectTypedObject()
               cancel()
            }
      } finally {
         cleanup(connection)
      }
   }

   @Test
   @Disabled("Redis Streams require stream setup and XADD operations")
   fun `can query a Redis Stream`(): Unit = runBlocking {
      val streamSchema = """
      import com.orbitalhq.redis.RedisService
      import com.orbitalhq.redis.RedisKey
      import com.orbitalhq.redis.RedisStreamName
      import com.orbitalhq.redis.RedisUpsertOperation

      @RedisKey(pattern = "film:{filmId}")
      @RedisStreamName(name = "films-stream")
      closed model Film {
         @Id
         filmId : FilmId inherits Int
         title : Title inherits String
      }

      @RedisService(connectionName = "test")
      service RedisService {
         stream films : Stream<Film>
      }
      """.trimIndent()

      val (connection, vyne, stub) = vyneWithRedis(streamSchema)
      try {
         // Add messages to the stream using XADD
         connection.sync().xadd("films-stream", mapOf("data" to """{"filmId":100,"title":"Star Wars"}"""))

         vyne.query("""stream { Film }""")
            .results
            .test(timeout = Duration.parse("5s")) {
               expectTypedObject()
               cancel()
            }
      } finally {
         cleanup(connection)
      }
   }

   private fun write(vyne: Vyne, valueToInsert: String) {
      runBlocking {
         val result = vyne.query(
            """
         given { film: Film = $valueToInsert }
         call RedisService::upsert
      """.trimIndent()
         )
            .firstRawObject()
         log().info("Wrote 1 record")
      }
   }

   private fun publishToChannel(
      connection: io.lettuce.core.api.StatefulRedisConnection<String, String>,
      channel: String,
      message: String
   ) {
      runBlocking {
         // Convert to JSON and publish
         val jsonMessage = """{"filmId":100,"title":"Star Wars"}"""
         connection.sync().publish(channel, jsonMessage)
         log().info("Published message to channel $channel")
      }
   }
}
