package com.orbitalhq.connectors.kafka

import com.orbitalhq.connectors.ConnectionSucceeded
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.utils.OBFUSCATED_VALUE
import com.orbitalhq.utils.get
import com.squareup.wire.durationOfSeconds
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG
import org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM
import org.apache.kafka.common.config.SslConfigs
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.stream.Collectors
import kotlin.random.Random


@Testcontainers
class KafkaSecureConnectionTest {
   // Based off how Testcontainers test this:
   // https://github.com/testcontainers/testcontainers-java/pull/7763/files

   @Test
   fun `cannot connect to Kafka with SASL_PLAIN without credentials`() {
      val kafka = secureKafkaContainer()

      kafka.use {
         kafka.start()
         val connection = KafkaConnectionConfiguration(
            "moviesConnection",
            kafka.bootstrapServers,
            "VyneTest-" + Random.nextInt(),
         )
         val testResult = KafkaConnection.test(connection, Duration.ofSeconds(5)).get()

         testResult.should.not.equal(ConnectionSucceeded)
         testResult.shouldBeInstanceOf<String>()
            // Not sure why we're getting timeout exceptions - suspect it's because we're connecting with the wrong
            // protocol.
            // Regardless, key point is that we didn't connect
            .shouldStartWith("org.apache.kafka.common.errors.TimeoutException:")
      }
   }

   @Test
   fun `can connect to Kafka with SASL_PLAIN with correct credentials`() {
      val kafka = secureKafkaContainer()

      kafka.use {
         kafka.start()
         val logs = kafka.getLogs()
         val connection = KafkaConnectionConfiguration(
            "moviesConnection",
            kafka.bootstrapServers,
            "VyneTest-" + Random.nextInt(),
            connectionParameters = mapOf(
               AdminClientConfig.SECURITY_PROTOCOL_CONFIG to "SASL_PLAINTEXT",
               SaslConfigs.SASL_MECHANISM to "PLAIN",
               SaslConfigs.SASL_JAAS_CONFIG to plainJaas("test", "secret")
            )

         )
         val testResult = KafkaConnection.test(connection, Duration.ofSeconds(30)).get()
         testResult.should.equal(ConnectionSucceeded)
      }
   }

   @Test
   fun `cannot connect to Kafka with SASL_PLAIN with incorrect credentials`() {
      val kafka = secureKafkaContainer()

      kafka.use {
         kafka.start()
         val logs = kafka.getLogs()
         val connection = KafkaConnectionConfiguration(
            "moviesConnection",
            kafka.bootstrapServers,
            "VyneTest-" + Random.nextInt(),
            connectionParameters = mapOf(
               AdminClientConfig.SECURITY_PROTOCOL_CONFIG to "SASL_PLAINTEXT",
               SaslConfigs.SASL_MECHANISM to "PLAIN",
               SaslConfigs.SASL_JAAS_CONFIG to plainJaas("test", "wrongPassword")
            )

         )
         val testResult = KafkaConnection.test(connection, Duration.ofSeconds(30)).get()
         testResult.shouldBeInstanceOf<String>()
            .shouldBe("org.apache.kafka.common.errors.SaslAuthenticationException: Authentication failed: Invalid username or password : Authentication failed: Invalid username or password")
      }
   }


   @Test
   fun obfuscatesCredentials() {
      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         "localhost:9029",
         "VyneTest-" + Random.nextInt(),
         connectionParameters = mapOf(
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to "./keystore",
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to "sslPassword",
            SaslConfigs.SASL_JAAS_CONFIG to plainJaas("test", "password")
         )
      )
      val displayProperties = connection.getUiDisplayProperties()
      displayProperties["ssl.key.password"].shouldBe(OBFUSCATED_VALUE)
      displayProperties["sasl.jaas.config"].shouldBe("""org.apache.kafka.common.security.plain.PlainLoginModule required username="test" password=******;""")
      displayProperties["ssl.keystore.location"].shouldBe("./keystore")
   }

   private val jaasConfig: String =
      """org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin" user_admin="admin" user_test="secret";"""


   private fun secureKafkaContainer(): KafkaContainer =
      KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
         .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT")
         .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
         .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
         .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
         .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", jaasConfig)
         .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", jaasConfig)

   private fun plainJaas(
      user: String, password: String,
   ): String =
      """org.apache.kafka.common.security.plain.PlainLoginModule required username="$user" password="$password";"""
}
