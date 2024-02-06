package com.orbitalhq.connectors.kafka

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.toReceiverOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.winterbe.expekt.should
import lang.taxi.types.StreamType
import org.junit.jupiter.api.Test
import kotlin.random.Random

class KafkaConfigExtensionTest {
   @Test
   fun `StreamQuery id set in query will converted to group id in kafka props`() {
      val connection = KafkaConnectionConfiguration(
         "myKafkaConnection",
         "localhost:9092",
         "VyneTest-" + Random.nextInt(),
      )

    val mockRemoteOperation = mock< RemoteOperation>()
    whenever(mockRemoteOperation.returnType).thenReturn(
       Type(
          "foo.bar.Me".fqn(),
          taxiType = StreamType.untyped(),
          typeDoc = null,
          sources = emptyList()
       )
    )
     val kafkaConsumerRequest =  KafkaConsumerRequest(
         "connectionName",
         "topic",
        KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST,
         mock {},
        mockRemoteOperation,
         streamSourceId = "consumerX"
      )
      connection.toReceiverOptions("latest", kafkaConsumerRequest).groupId().should.equal("consumerX")
   }
}
