package com.orbitalhq.connectors.redis.invoker

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.config.redis.RedisConfiguration
import com.orbitalhq.connectors.redis.RedisConnectionProvider
import com.orbitalhq.connectors.redis.RedisTaxi
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyneWithStub
import io.lettuce.core.api.StatefulRedisConnection
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

abstract class BaseRedisInvokerTest {
   companion object {
      // Shared Redis container for all tests
      val redisContainer: KGenericContainer by lazy {
         KGenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .apply { start() }
      }
   }

   open fun defaultSchema(): String = """
         import com.orbitalhq.redis.RedisService
         import com.orbitalhq.redis.RedisKey
         import com.orbitalhq.redis.RedisTTL
         import com.orbitalhq.redis.RedisUpsertOperation
         import com.orbitalhq.redis.RedisDeleteOperation

         model Person {
            name : PersonName inherits String
         }
         type Language inherits String

         @RedisKey(pattern = "film:{filmId}")
         closed model Film {
            @Id
            filmId : FilmId inherits Int
            title : Title inherits String
            languages: Language[]
            director : Person
            cast : Person[]
         }

         @RedisService(connectionName = "test")
         service RedisService {
            @RedisUpsertOperation
            write operation upsert(Film):Film

            @RedisDeleteOperation(keyPattern = "film:*")
            write operation deleteAll()

            @RedisDeleteOperation(keyPattern = "film:{filmId}")
            write operation delete(FilmId):Film

            table films : Film[]
         }
         """

   fun vyneWithRedis(schema: String = defaultSchema()): Triple<StatefulRedisConnection<String, String>, Vyne, StubService> {
      // Create real Redis connection to test container
      val connection = io.lettuce.core.RedisClient.create(
         "redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}"
      ).connect()

      // Clear any existing data
      connection.sync().flushdb()

      val redisProvider = TestRedisProvider(connection)
      val redisInvoker = RedisInvoker(redisProvider)
      val (vyne, stub) = testVyneWithStub(
         TaxiSchema.fromStrings(
            listOf(
               VyneQlGrammar.QUERY_TYPE_TAXI,
               RedisTaxi.schema,
               schema,
            )
         ), listOf(redisInvoker)
      )
      return Triple(connection, vyne, stub)
   }

   // Helper to clean up connection
   fun cleanup(connection: StatefulRedisConnection<String, String>) {
      connection.sync().flushdb()
      connection.close()
   }
}

// Kotlin-friendly wrapper for Testcontainers
class KGenericContainer(imageName: DockerImageName) : GenericContainer<KGenericContainer>(imageName)

/**
 * Test provider that supplies a real Redis connection for testing
 */
class TestRedisProvider(
   private val connection: StatefulRedisConnection<String, String>
) : RedisConnectionProvider {
   override fun provide(config: RedisConfiguration): StatefulRedisConnection<String, String> {
      return connection
   }

   override fun redisConnection(connectionName: String?): Pair<StatefulRedisConnection<String, String>, RedisConfiguration> {
      val redisConfiguration: RedisConfiguration = mock { }
      whenever(redisConfiguration.connectionName).doReturn(connectionName ?: "test")
      whenever(redisConfiguration.addresses).doReturn(listOf("localhost:6379"))
      return connection to redisConfiguration
   }

   override fun canProvideRedisConnection(connectionName: String?): Boolean {
      return true
   }
}
