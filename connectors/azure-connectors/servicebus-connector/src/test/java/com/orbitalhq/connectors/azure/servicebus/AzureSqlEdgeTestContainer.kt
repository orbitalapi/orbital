package com.orbitalhq.connectors.azure.servicebus

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class AzureSqlEdgeTestContainer(val password: String, private val serviceBusNetwork: Network): GenericContainer<AzureSqlEdgeTestContainer>("mcr.microsoft.com/azure-sql-edge:latest") {

    override fun configure() {
        withEnv("MSSQL_SA_PASSWORD",password)
        withEnv("ACCEPT_EULA", "Y")
        withNetwork(serviceBusNetwork)
    }
}