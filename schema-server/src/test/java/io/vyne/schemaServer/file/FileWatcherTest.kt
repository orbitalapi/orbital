package io.vyne.schemaServer.file

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockitokotlin2.verify
import io.vyne.schemaServer.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemaStore.SchemaPublisher
import org.junit.After
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
         watcher.cancelWatch()
         watcher.stop()
      }
   }

   @Test
   fun `file watcher detects changes to existing file`() {
      val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
      createdFile.toFile().writeText("Hello, world")
      val (sourceWatchingSchemaPublisher, _) = newWatcher()

      createdFile.toFile().writeText("Hello, cruel world")

      verify(sourceWatchingSchemaPublisher, timeout(3000)).submitSchemas(any())
   }

   @Test
   fun `file watcher detects new file created`() {
      val (sourceWatchingSchemaPublisher, _) = newWatcher()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(sourceWatchingSchemaPublisher, timeout(3000).atLeast(1)).submitSchemas(any())
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (sourceWatchingSchemaPublisher, _) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")
      expectRecompilationTriggered(sourceWatchingSchemaPublisher)
   }

   @Test
   fun `handles new nested folder`() {
      val (sourceWatchingSchemaPublisher, _) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")
      expectRecompilationTriggered(sourceWatchingSchemaPublisher)
   }

   private fun expectRecompilationTriggered(sourceWatchingSchemaPublisher: SchemaPublisher) {
      verify(sourceWatchingSchemaPublisher, timeout(3000)).submitSchemas(any())
      reset(sourceWatchingSchemaPublisher)
   }

   private fun newWatcher(): Pair<SchemaPublisher, FileWatcher> {
      val schemaPublisher = mock<SchemaPublisher>()

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