package io.vyne.connectors.azure.blob.support

import org.testcontainers.containers.GenericContainer

class AzuriteContainer: GenericContainer<AzuriteContainer>("mcr.microsoft.com/azure-storage/azurite")
