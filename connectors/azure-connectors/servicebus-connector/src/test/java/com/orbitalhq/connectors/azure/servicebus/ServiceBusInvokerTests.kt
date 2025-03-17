package com.orbitalhq.connectors.azure.servicebus

import com.orbitalhq.connectors.azure.servicebus.AzureServiceBusEmulatorContainer.Companion.testConnectionName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultSubscriptionName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultTestQueueName
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultTestTopicName
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.winterbe.expekt.should
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier


class ServiceBusInvokerTests: ServiceBusTestBase() {

    @Test
    fun `can publish to a service bus topic and stream the data from the topic`():Unit = runTest {
        val personSchema =
            """
         ${ServiceBusTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         model Person {
            name : Name inherits String
         }
         service PersonApi {
            operation getPerson():Person
         }
          @ServiceBusService( connectionName = "$testConnectionName" )
          service PersonServiceBusService {
             @ServiceBusTopicSubscriptionOperation( topic = "$DefaultTestTopicName", subscription = "$DefaultSubscriptionName" )
             stream streamMovieQuery:Stream<Person>

             @ServiceBusTopicPublicationOperation( topic = "$DefaultTestTopicName" )
             write operation publishMessage(Person):Person
          }
      """
        val (vyne, stub) = vyneWithServiceBusInvoker(personSchema)
        stub.addResponse("getPerson", vyne.parseJson("Person","""{ "name" : "Jimmy" }"""))

        val result = vyne.query("""
         find { Person }
         call PersonServiceBusService::publishMessage
      """.trimIndent())
            .firstRawObject()

        result["name"].should.equal("Jimmy")

        val subscriptionResult = vyne.query("""
         stream { Person }
      """.trimIndent())
            .results.asPublisher()

        StepVerifier.create(subscriptionResult)
            .expectSubscription()
            .expectNextMatches { typedInstance ->
                val typedObject = typedInstance as TypedObject
                typedObject["name"].value == "Jimmy"
            }.thenCancel()
            .verify()
    }

    @Test
    fun `can publish to a service bus queue and stream the data from the queue`():Unit = runTest {
        val personSchema =
            """
         ${ServiceBusTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         model Person {
            name : Name inherits String
         }
         service PersonApi {
            operation getPerson():Person
         }
          @ServiceBusService( connectionName = "$testConnectionName" )
          service PersonServiceBusService {
             @ServiceBusQueueOperation( queue = "$DefaultTestQueueName")
             stream streamMovieQuery:Stream<Person>

             @ServiceBusQueueOperation( queue = "$DefaultTestQueueName" )
             write operation publishMessage(Person):Person
          }
      """
        val (vyne, stub) = vyneWithServiceBusInvoker(personSchema)
        stub.addResponse("getPerson", vyne.parseJson("Person","""{ "name" : "Jimmy" }"""))

        val result = vyne.query("""
         find { Person }
         call PersonServiceBusService::publishMessage
      """.trimIndent())
            .firstRawObject()

        result["name"].should.equal("Jimmy")

        val subscriptionResult = vyne.query("""
         stream { Person }
      """.trimIndent())
            .results.asPublisher()

        StepVerifier.create(subscriptionResult)
            .expectSubscription()
            .expectNextMatches { typedInstance ->
                val typedObject = typedInstance as TypedObject
                typedObject["name"].value == "Jimmy"
            }.thenCancel()
            .verify()
    }
}