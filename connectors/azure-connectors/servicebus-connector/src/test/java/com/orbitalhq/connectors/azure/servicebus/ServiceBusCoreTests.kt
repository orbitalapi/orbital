package com.orbitalhq.connectors.azure.servicebus

import com.azure.core.util.BinaryData
import com.azure.messaging.servicebus.ServiceBusMessage
import com.orbitalhq.connectors.azure.servicebus.AzureServiceBusEmulatorContainer.Companion.testConnectionName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultSubscriptionName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultTestQueueName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultTestTopicName
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class ServiceBusCoreTests: ServiceBusTestBase() {
    @Test
    fun `can publish and receive from a queue`() {

        val asyncSender = serviceBusConnectionFactory.queueSender(testConnectionName, DefaultTestQueueName)

        StepVerifier.create(asyncSender.sendMessage(ServiceBusMessage(BinaryData.fromString("test"))))
            .expectSubscription()
            .expectComplete()
            .verify()

        val asyncReceiver = serviceBusConnectionFactory.queueReceiver(testConnectionName, DefaultTestQueueName)
        StepVerifier.create(asyncReceiver.receiveMessages())
            .expectSubscription()
            .expectNextMatches { receivedMessage ->
                receivedMessage.body.toString() == "test"
            }
            .thenCancel()
            .verify()
    }

    @Test
    fun `can publish and receive from a topic`() {

        val asyncSender = serviceBusConnectionFactory.topicSender(testConnectionName, DefaultTestTopicName)

        StepVerifier.create(asyncSender.sendMessage(ServiceBusMessage(BinaryData.fromString("test"))))
            .expectSubscription()
            .expectComplete()
            .verify()

        val asyncReceiver = serviceBusConnectionFactory.topicReceiver(testConnectionName, DefaultTestTopicName, DefaultSubscriptionName)
        StepVerifier.create(asyncReceiver.receiveMessages())
            .expectSubscription()
            .expectNextMatches { receivedMessage ->
                receivedMessage.body.toString() == "test"
            }
            .thenCancel()
            .verify()

    }
}