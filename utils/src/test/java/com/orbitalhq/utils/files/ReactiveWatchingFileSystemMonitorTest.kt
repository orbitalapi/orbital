package com.orbitalhq.utils.files

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.test.StepVerifier
import java.io.File
import java.time.Duration

class ReactiveWatchingFileSystemMonitorTest {
    @TempDir
    private lateinit var folder: File

    private var monitor: ReactiveWatchingFileSystemMonitor? = null

    @AfterEach
    fun cleanup() {
        monitor?.stop()
    }

    @Test
    fun `can monitor file changes`() {
        val taxiFile = folder.resolve("account.taxi")
        taxiFile.writeText("type AccountName inherits String")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath())
        val fileChangedEvents = monitor!!.startWatching()

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Update the existing file
                taxiFile.appendText("\ntype AccountId inherits String")
            }
            .expectNextMatches { events ->
                events.isNotEmpty() && events.any { it.path.fileName.toString() == "account.taxi" }
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `should ignore git directory files`() {
        val gitDir = folder.resolve(".git")
        gitDir.mkdir()
        val gitFile = gitDir.resolve("config")

        val taxiFile = folder.resolve("schema.taxi")
        taxiFile.writeText("type Schema inherits String")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath())
        val fileChangedEvents = monitor!!.startWatching()

        var capturedEvents: List<FileSystemChangeEvent>? = null

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Create a git file (should be ignored)
                gitFile.writeText("git config content")
                Thread.sleep(100)
                // Create a taxi file (should be detected)
                taxiFile.appendText("\ntype Updated inherits String")
            }
            .expectNextMatches { events ->
                capturedEvents = events
                events.isNotEmpty()
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        // Verify only the taxi file was detected, not the git file
        capturedEvents?.let { events ->
            assertTrue(events.none { it.path.toString().contains(".git") })
            assertTrue(events.any { it.path.fileName.toString() == "schema.taxi" })
        }
    }

    @Test
    fun `should ignore vim swap files`() {
        val taxiFile = folder.resolve("schema.taxi")
        taxiFile.writeText("type Schema inherits String")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath())
        val fileChangedEvents = monitor!!.startWatching()

        var capturedEvents: List<FileSystemChangeEvent>? = null

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Create a swap file (should be ignored)
                folder.resolve("schema.taxi.swp").writeText("swap content")
                Thread.sleep(100)
                // Update the taxi file (should be detected)
                taxiFile.appendText("\ntype Updated inherits String")
            }
            .expectNextMatches { events ->
                capturedEvents = events
                events.isNotEmpty()
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        // Verify only the taxi file was detected, not the swap file
        capturedEvents?.let { events ->
            assertTrue(events.none { it.path.fileName.toString().endsWith(".swp") })
            assertTrue(events.any { it.path.fileName.toString() == "schema.taxi" })
        }
    }

    @Test
    fun `should ignore macOS temporary files`() {
        val taxiFile = folder.resolve("schema.taxi")
        taxiFile.writeText("type Schema inherits String")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath())
        val fileChangedEvents = monitor!!.startWatching()

        var capturedEvents: List<FileSystemChangeEvent>? = null

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Create a macOS temp file (should be ignored)
                folder.resolve("._schema.taxi").writeText("temp content")
                Thread.sleep(100)
                // Update the taxi file (should be detected)
                taxiFile.appendText("\ntype Updated inherits String")
            }
            .expectNextMatches { events ->
                capturedEvents = events
                events.isNotEmpty()
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        // Verify only the taxi file was detected, not the macOS temp file
        capturedEvents?.let { events ->
            assertTrue(events.none { it.path.fileName.toString().startsWith("._") })
            assertTrue(events.any { it.path.fileName.toString() == "schema.taxi" })
        }
    }

    @Test
    fun `should ignore tmp files`() {
        val taxiFile = folder.resolve("schema.taxi")
        taxiFile.writeText("type Schema inherits String")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath())
        val fileChangedEvents = monitor!!.startWatching()

        var capturedEvents: List<FileSystemChangeEvent>? = null

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Create a tmp file (should be ignored)
                folder.resolve("temp.tmp").writeText("temp content")
                Thread.sleep(100)
                // Update the taxi file (should be detected)
                taxiFile.appendText("\ntype Updated inherits String")
            }
            .expectNextMatches { events ->
                capturedEvents = events
                events.isNotEmpty()
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        // Verify only the taxi file was detected, not the tmp file
        capturedEvents?.let { events ->
            assertTrue(events.none { it.path.fileName.toString().endsWith(".tmp") })
            assertTrue(events.any { it.path.fileName.toString() == "schema.taxi" })
        }
    }

    @Test
    fun `should only monitor files with valid extensions`() {
        val taxiFile = folder.resolve("schema.taxi")
        val txtFile = folder.resolve("readme.txt")

        taxiFile.writeText("type Schema inherits String")
        txtFile.writeText("Some readme content")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath())
        val fileChangedEvents = monitor!!.startWatching()

        var capturedEvents: List<FileSystemChangeEvent>? = null

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Update both files
                txtFile.appendText("\nMore content")
                Thread.sleep(50)
                taxiFile.appendText("\ntype Updated inherits String")
            }
            .expectNextMatches { events ->
                capturedEvents = events
                events.isNotEmpty()
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        // Verify only the taxi file was detected, not the txt file
        capturedEvents?.let { events ->
            assertTrue(events.none { it.path.fileName.toString() == "readme.txt" })
            assertTrue(events.any { it.path.fileName.toString() == "schema.taxi" })
        }
    }

    @Test
    fun `should respect excluded directory names`() {
        val nodeModulesDir = folder.resolve("node_modules")
        nodeModulesDir.mkdir()
        val nodeFile = nodeModulesDir.resolve("package.json")

        val taxiFile = folder.resolve("schema.taxi")
        taxiFile.writeText("type Schema inherits String")

        monitor = ReactiveWatchingFileSystemMonitor(folder.toPath(), excludedDirectoryNames = listOf("node_modules"))
        val fileChangedEvents = monitor!!.startWatching()

        var capturedEvents: List<FileSystemChangeEvent>? = null

        StepVerifier
            .create(fileChangedEvents)
            .expectSubscription()
            .then {
                Thread.sleep(100) // Give watcher time to start
                // Create a file in node_modules (should be ignored)
                nodeFile.writeText("{\"name\": \"test\"}")
                Thread.sleep(100)
                // Update the taxi file (should be detected)
                taxiFile.appendText("\ntype Updated inherits String")
            }
            .expectNextMatches { events ->
                capturedEvents = events
                events.isNotEmpty()
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))

        // Verify only the taxi file was detected, not files in node_modules
        capturedEvents?.let { events ->
            assertTrue(events.none { it.path.toString().contains("node_modules") })
            assertTrue(events.any { it.path.fileName.toString() == "schema.taxi" })
        }
    }
}
