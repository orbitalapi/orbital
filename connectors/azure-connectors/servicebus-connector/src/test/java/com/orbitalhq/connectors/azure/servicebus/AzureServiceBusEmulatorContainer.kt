package com.orbitalhq.connectors.azure.servicebus

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode
import com.github.dockerjava.api.command.LogContainerCmd
import com.orbitalhq.connectors.azure.servicebus.ServiceBusTestUtils.DefaultWarmUpQueue
import com.orbitalhq.connectors.config.azure.ServiceBusConnectionConfiguration
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.FrameConsumerResultCallback
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.WaitingConsumer
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Predicate

class AzureServiceBusEmulatorContainer(private val serviceBusConfigFile: String = "service-bus-config.json"):
    GenericContainer<AzureServiceBusEmulatorContainer>(DockerImageName.parse("mcr.microsoft.com/azure-messaging/servicebus-emulator:latest")) {

    private val network: Network = Network.newNetwork()
    private val msSqlServerContainer = AzureSqlEdgeTestContainer("yourStrong(!)Password", network)
        .withNetworkAliases("sqlEdge")

    init {
        dependsOn(msSqlServerContainer)
        withExposedPorts(DEFAULT_PORT)
        waitingFor(ServiceBusContainerWaitStrategy(msSqlServerContainer))
    }
    companion object {
        //"Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"
         const val ConnectionStringFormat =
            "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;"

        const val DEFAULT_PORT = 5672

        const val testConnectionName = "testConnection"
    }

    override fun configure() {
        withEnv("SQL_SERVER", msSqlServerContainer.networkAliases.first())
        withEnv("MSSQL_SA_PASSWORD", msSqlServerContainer.password)
        withEnv("ACCEPT_EULA", "Y")
        withNetwork(network)
        withCopyFileToContainer(MountableFile.forClasspathResource(serviceBusConfigFile),"/ServiceBus_Emulator/ConfigFiles/Config.json")
    }

    val connectionString: String
        get() = ConnectionStringFormat.format(host, getMappedPort(DEFAULT_PORT))

    val serviceBusConnectionConfiguration: ServiceBusConnectionConfiguration
        get() {
            return  ServiceBusConnectionConfiguration(
                testConnectionName,
                "RootManageSharedAccessKey",
                "SAS_KEY_VALUE",
                "$host:${getMappedPort(DEFAULT_PORT)}",
                mapOf("UseDevelopmentEmulator" to "true")
            )
        }


}

/***
 * WHY do we have this mess?
 *
 * ServiceBus Container depends on Ms Sql Edge container, but there seems to additional wait requirement:
 *
 *  1. MsSql starts up
 *  2. Service Bus Starts Up - This can be verified with `Emulator Service is Successfully Up!` log message.
 *  3. Service Bus talks to MsSql and starts creating two databases - SbGatewayDatabase and SbMessageContainerDatabase00001
 *  4. ServiceBus does some mysterious post initialisation step to make the queues and topics ready
 *
 *  So, we need to have a custom Wait Strategy that waits for both 2,  3 and 4. Waiting on only for step 2 creates unstable tests.
 */
class ServiceBusContainerWaitStrategy(private val edgeContainer: AzureSqlEdgeTestContainer): AbstractWaitStrategy() {

    private val regEx: String = ".*Emulator Service is Successfully Up!.*"
    private val edgeContainerRegEx: String = ".*started for database 'SbMessageContainerDatabase00001' with worker pool.*"
    private val times = 1

    private fun waitUntilTestQueueIsAvailable() {
       val serviceBusConnectionString =  AzureServiceBusEmulatorContainer.ConnectionStringFormat.format(this.waitStrategyTarget.host, this.waitStrategyTarget.getMappedPort(
            AzureServiceBusEmulatorContainer.DEFAULT_PORT
        ))

        try {
            Unreliables.retryUntilTrue(
                startupTimeout.seconds.toInt(), TimeUnit.SECONDS
            ) {
                val sender = ServiceBusClientBuilder()
                    .connectionString(serviceBusConnectionString)
                    .sender()
                    .queueName(DefaultWarmUpQueue)
                    .buildClient()

                val receiver = ServiceBusClientBuilder()
                    .connectionString(serviceBusConnectionString)
                    .receiver()
                    .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
                    .prefetchCount(1)
                    .queueName(DefaultWarmUpQueue)
                    .buildClient()

                try {
                    sender.sendMessage(ServiceBusMessage("test"))
                    receiver.receiveMessages(1)
                    true

                } catch (e: Exception) {
                    false
                }finally {
                    sender.close()
                    receiver.close()

                }
            }
        } catch (e: org.rnorth.ducttape.TimeoutException) {
            throw ContainerLaunchException("Timed out waiting for container to execute Service Bus warm-up" )
        }

    }

    private fun doWaitForLogMessage(cmd: LogContainerCmd, searchRegex: String) {
        try {
            val waitingConsumer = WaitingConsumer()
            val callback = FrameConsumerResultCallback()
            var outerException: Throwable? = null
            try {
                callback.addConsumer(OutputFrame.OutputType.STDOUT, waitingConsumer)
                callback.addConsumer(OutputFrame.OutputType.STDERR, waitingConsumer)
                cmd.exec(callback)
                val waitPredicate =
                    Predicate<OutputFrame> { outputFrame: OutputFrame ->
                        outputFrame.utf8String.matches(("(?s)$searchRegex").toRegex())
                    }
                try {
                    waitingConsumer.waitUntil(waitPredicate, startupTimeout.seconds, TimeUnit.SECONDS, this.times)
                } catch (e: TimeoutException) {
                    throw ContainerLaunchException("Timed out waiting for log output matching $searchRegex")
                }
            } catch (e: Throwable) {
                outerException = e
                throw e
            } finally {
                if (outerException != null) {
                    try {
                        callback.close()
                    } catch (e: Throwable) {
                        outerException.addSuppressed(e)
                    }
                } else {
                    callback.close()
                }
            }
        } catch (e: IOException) {
            throw e
        }

    }
    override fun waitUntilReady() {
        //Wait For Service Bus container to initialise.
        val cmd: LogContainerCmd =
            this.waitStrategyTarget
                .dockerClient
                .logContainerCmd(this.waitStrategyTarget.containerId)
                .withFollowStream(true)
                .withSince(0)
                .withStdOut(true)
                .withStdErr(true)
        this.doWaitForLogMessage(cmd, this.regEx)

        //Wait For Service Bus to create its databases in MsSql.
        val edgeContainerLogContainerCmd =
            this.edgeContainer
                .dockerClient
                .logContainerCmd(this.edgeContainer.containerId)
                .withFollowStream(true)
                .withSince(0)
                .withStdOut(true)
                .withStdErr(true)
        this.doWaitForLogMessage(edgeContainerLogContainerCmd, this.edgeContainerRegEx)

        // Wait For Service Bus to initialise Test Queue.
        this.waitUntilTestQueueIsAvailable()
    }

}