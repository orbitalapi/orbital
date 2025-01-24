package com.orbitalhq.connectors.azure.servicebus

import com.orbitalhq.Vyne
import com.orbitalhq.connectors.StreamErrorPublisher
import com.orbitalhq.connectors.azure.servicebus.registry.InMemoryServiceBusConnectionRegistry
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.models.format.DefaultFormatRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyneWithStub
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract  class ServiceBusTestBase {
    lateinit var connectionRegistry: InMemoryServiceBusConnectionRegistry
    lateinit var serviceBusConnectionFactory: ServiceBusConnectionFactory
    val formatRegistry = DefaultFormatRegistry(listOf())

    companion object {
        @JvmField
        @Container
        val serviceBusContainer = AzureServiceBusEmulatorContainer()
    }

    @BeforeEach
    fun beforeEach() {
        connectionRegistry = InMemoryServiceBusConnectionRegistry(listOf(serviceBusContainer.serviceBusConnectionConfiguration))
        serviceBusConnectionFactory = ServiceBusConnectionFactory(connectionRegistry)
    }

    fun vyneWithServiceBusInvoker(taxi: String): ServiceBusTestSetup {
        val schema = TaxiSchema.fromStrings(
            listOf(
                ServiceBusTaxi.schema,
                ErrorType.queryErrorVersionedSource.content,
                VyneQlGrammar.QUERY_TYPE_TAXI,
                taxi
            )
        )
        return vyneWithServiceBusInvoker(schema)
    }

    fun vyneWithServiceBusInvoker(schema: TaxiSchema): ServiceBusTestSetup {
        val schemaProvider =  SimpleSchemaProvider(schema)
        val meterRegistry  = SimpleMeterRegistry()
        val serviceBusPublisher = ServiceBusPublisher(serviceBusConnectionFactory, formatRegistry, meterRegistry)
        val serviceBusReceiver= ServiceBusReceiver(serviceBusConnectionFactory, formatRegistry, meterRegistry)
        val streamErrorPublisher = StreamErrorPublisher()
        val invokers = listOf(
            ServiceBusInvoker(schemaProvider, serviceBusPublisher, serviceBusReceiver, streamErrorPublisher)
        )
        val (vyne, stub) = testVyneWithStub(schema, invokers)
        return ServiceBusTestSetup(vyne, stub, streamErrorPublisher)
    }

}

data class ServiceBusTestSetup(
    val vyne: Vyne,
    val stubService: StubService,
    val streamErrorPublisher: StreamErrorPublisher
)