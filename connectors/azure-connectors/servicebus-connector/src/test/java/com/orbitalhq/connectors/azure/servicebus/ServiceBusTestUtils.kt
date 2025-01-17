package com.orbitalhq.connectors.azure.servicebus

object ServiceBusTestUtils {
    // see 'resources/service-bus-config.json' for the configured queue name
    const val DefaultTestQueueName = "queue.1"
    const val DefaultTestTopicName = "topic.1"
    const val DefaultSubscriptionName = "subscription.3"
    const val DefaultWarmUpQueue = "warmupqueue.1"
}