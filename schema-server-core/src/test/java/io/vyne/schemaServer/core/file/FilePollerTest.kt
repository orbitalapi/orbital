package io.vyne.schemaServer.core.file

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockito_kotlin.verify
import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import mu.KotlinLogging
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.time.Duration

class FilePollerTest {

   private val logger = KotlinLogging.logger {}
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   private lateinit var poller: FilePoller
   private lateinit var watchingPublisher: SourceWatchingSchemaPublisher

   @Test
   fun `file watcher detects changes to existing file`() {
      val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
      createdFile.toFile().writeText("Hello, world")
      val (fileChangeSchemaPublisher, watcher) = newWatcher()

      createdFile.toFile().writeText("Hello, cruel world")

      watcher.poll()
      verify(fileChangeSchemaPublisher, timeout(3000)).submitSchemas(any())
   }

   @Test
   fun `file watcher detects new file created`() {
      val (schemaPublisher, watcher) = newWatcher()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      watcher.poll()
      verify(schemaPublisher, timeout(3000)).submitSchemas(any())
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (schemaPublisher, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      watcher.poll()
      verify(schemaPublisher, timeout(3000).atLeast(1)).submitSchemas(any())
   }

   @Test
   fun `handles new nested folder`() {
      val (schemaPublisher, watcher) = newWatcher()
      reset(schemaPublisher)

      val newDir = folder.newFolder("newDir").toPath()
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      watcher.poll()
      reset(schemaPublisher)

      // modify a nested file..
      val nestedFile = nestedDir.resolve("hello.taxi").toFile()
      logger.info { "Writing to nested file ${nestedFile.canonicalPath}" }
      nestedFile.writeText("Hello, world")
      watcher.poll()

      verify(schemaPublisher, atLeast(1)).submitSchemas(any())
   }

   private fun newWatcher(): Pair<SchemaPublisherTransport, FilePoller> {
      val schemaPublisher = mock<SchemaPublisherTransport>()
      val repository = FileSystemSchemaRepository.forPath(folder.root.toPath())
      poller = FilePoller(
         repository,
         Duration.ofMillis(10000)
      )
      poller.start()
      watchingPublisher = SourceWatchingSchemaPublisher(listOf(repository), schemaPublisher)
      // Wait a bit, to let the watcher get started before we do anything
      Thread.sleep(500)
      return schemaPublisher to poller
   }

   @After
   fun stopPoller() {
      poller.stop()
   }
}
