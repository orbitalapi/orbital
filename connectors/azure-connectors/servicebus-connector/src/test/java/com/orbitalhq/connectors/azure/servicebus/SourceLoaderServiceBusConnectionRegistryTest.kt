package com.orbitalhq.connectors.azure.servicebus

import com.google.common.io.Resources
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.winterbe.expekt.should
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SourceLoaderServiceBusConnectionRegistryTest {
    @TempDir
    lateinit var folder: Path
    @Test
    fun `can load kafka connection from config file registry`() {
        val configFilePath = configFileInTempFolder("test-connections.conf")
        val fileConfigSourceLoader = FileConfigSourceLoader(
            configFilePath,
            packageIdentifier = FileConfigSourceLoader.LOCAL_PACKAGE_IDENTIFIER
        )

        val registry = SourceLoaderConnectorsRegistry(listOf(fileConfigSourceLoader))
        val serviceBusConnectionConfigurations = registry.load().serviceBus

        val warehouseServiceBusConnection = serviceBusConnectionConfigurations["warehouseConnection"]
        warehouseServiceBusConnection!!.connectionName.should.equal("warehouseConnection")
        warehouseServiceBusConnection.connectionString.should.equal("Endpoint=sb://foo.servicebus.windows.net/;SharedAccessKeyName=accessKeyName;SharedAccessKey=accessKey;")
        warehouseServiceBusConnection.optionalParameters.should.be.empty
    }

    private fun configFileInTempFolder(resourceName: String): Path {
        val content =  Resources.getResource(resourceName).readText()
        val targetFile = folder.resolve(resourceName).toFile()
        targetFile.writeText(content)
        return targetFile.toPath()
    }
}
