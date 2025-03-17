package com.orbitalhq.connectors.azure.servicebus

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class ServiceBusBatchTests: ServiceBusTestBase() {
    @Test
    fun `can batch publish and receive from a queue`(): Unit = runTest {

        val personSchema =
            """
         ${ServiceBusTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         model Person {
            name : Name inherits String
         }
         service PersonApi {
            operation getPerson():Person[]
         }
          @ServiceBusService( connectionName = "${AzureServiceBusEmulatorContainer.testConnectionName}" )
          service PersonServiceBusService {
             @ServiceBusTopicSubscriptionOperation( topic = "${ServiceBusTestUtils.DefaultTestTopicName}", subscription = "${ServiceBusTestUtils.DefaultSubscriptionName}" )
             stream streamMovieQuery:Stream<Person>

             @ServiceBusTopicPublicationOperation( topic = "${ServiceBusTestUtils.DefaultTestTopicName}", batchSize = 5, batchDuration = 10000  )
             write operation publishMessage(Person):Person
          }
      """
        val (vyne, stub) = vyneWithServiceBusInvoker(personSchema)
        stub.addResponse("getPerson", vyne.parseJson("Person[]",
            """[{ "name" : "Jimmy" }, { "name" : "Tony" }, { "name" : "Axl" }, { "name" : "Lars" }, { "name" : "Boris" }]"""))

        val result = vyne.query("""
         find { Person[] }
         call PersonServiceBusService::publishMessage
      """.trimIndent())
            .results.asPublisher()

        StepVerifier.create(result)
            .recordWith { mutableListOf<TypedInstance>() }
            .thenConsumeWhile { true }
            .expectRecordedMatches {
                val personNames = it.map { typedInstance ->
                    val typedObject = typedInstance as TypedObject
                    typedObject["name"].value!!.toString()
                }.toSet().sorted()

                personNames == setOf("Jimmy", "Tony", "Axl", "Lars", "Boris").sorted()
            }
            .verifyComplete()

    }
}