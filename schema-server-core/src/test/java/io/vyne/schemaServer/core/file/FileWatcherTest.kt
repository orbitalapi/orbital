package io.vyne.schemaServer.core.file

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockito_kotlin.verify
import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.time.Duration

// I've tried to make this test as safe as possible to avoid timing
// related issues.  FileWatchers run on their own threads, and
// the scheduling is very OS dependant, so we could start to see intermittent
// failures.  If so, revisit these tests.
class FileWatcherTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var watcher: FileWatcher
   lateinit var watchingPublisher: SourceWatchingSchemaPublisher

   @After
   fun tearDown() {
      if (this::watcher.isInitialized) {
         watcher.stop()
      }
   }

   @Before
   fun setup() {
      folder.deployProject("sample-project")
   }

   @Test
   fun `file watcher detects changes to existing file`() {
      val createdFile = Files.createFile(folder.root.toPath().resolve("src/hello.taxi"))
      createdFile.toFile().writeText("Hello, world")
      val (sourceWatchingSchemaPublisher, _) = newWatcher()

      createdFile.toFile().writeText("Hello, cruel world")

      verify(sourceWatchingSchemaPublisher, timeout(Duration.ofSeconds(3)).atLeastOnce()).submitPackages(any())
   }

   @Test
   fun `file watcher detects new file created`() {
      val (sourceWatchingSchemaPublisher, _) = newWatcher()
      val createdFile = folder.root.toPath().resolve("src/hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(sourceWatchingSchemaPublisher, timeout(Duration.ofSeconds(11)).atLeastOnce()).submitPackages(any())
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (sourceWatchingSchemaPublisher, _) = newWatcher()

      val newDir = folder.newFolder("src/newDir").toPath()
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")
      expectRecompilationTriggered(sourceWatchingSchemaPublisher)
   }

   @Test
   fun `handles new nested folder`() {
      val (sourceWatchingSchemaPublisher, _) = newWatcher()

      val newDir = folder.newFolder("src/newDir").toPath()
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")
      expectRecompilationTriggered(sourceWatchingSchemaPublisher)
   }

   private fun expectRecompilationTriggered(sourceWatchingSchemaPublisher: SchemaPublisherTransport) {
      verify(sourceWatchingSchemaPublisher, timeout(Duration.ofSeconds(11)).atLeastOnce()).submitPackages(any())
      reset(sourceWatchingSchemaPublisher)
   }

   private fun newWatcher(): Pair<SchemaPublisherTransport, FileWatcher> {
      val schemaPublisher = mock<SchemaPublisherTransport>()

      val repository = FileSystemSchemaRepository.forPath(folder.root.canonicalPath)
      val watcher = FileWatcher(
         repository,
         Duration.ofMillis(1)
      )
      watchingPublisher = SourceWatchingSchemaPublisher(
         listOf(repository),
         schemaPublisher
      )
      watcher.start()

      // Wait a bit, to let the watcher get started before we do anything
      Thread.sleep(500)
      return schemaPublisher to watcher
   }
}

private fun timeout(duration: Duration) = timeout(duration.toMillis())
