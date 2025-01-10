package com.orbitalhq.utils.files

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.test.StepVerifier
import java.io.File
import java.time.Duration

class ReactivePollingFileSystemMonitorTest {
    @TempDir
    private lateinit var folder: File
    @Test
    fun `can monitor file changes`() {
        // Create a taxi file.
         val taxiFile = folder
               .resolve("account.taxi")

        taxiFile.writeText("type AccountName inherits String")

        val reactiveFileMonitor = ReactivePollingFileSystemMonitor(folder.toPath(), Duration.ofSeconds(1))
        // Start watching changes
        val fileChangedEvents =  reactiveFileMonitor.startWatching()

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                reactiveFileMonitor.suspend()
                // update the existing file
                taxiFile.appendText("type AccountId inherits String")
                // create a new file.
                folder.resolve("customer.taxi").writeText("type ClientId inherits Int")
                reactiveFileMonitor.resume()
            }.expectNextMatches { it.size == 1 }
            .thenCancel()
            .verify()
    }
}