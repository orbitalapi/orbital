package io.vyne.schemaServer

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockitokotlin2.verify
import io.vyne.schemaServer.file.FileChangeSchemaPublisher
import io.vyne.schemaServer.file.FilePoller
import io.vyne.schemaServer.file.FileSystemVersionedSourceLoader
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class FilePollerTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   private lateinit var poller: FilePoller

   @Test
   fun `file watcher detects changes to existing file`() {
      val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
      createdFile.toFile().writeText("Hello, world")
      val (fileChangeSchemaPublisher: FileChangeSchemaPublisher, watcher) = newWatcher()

      createdFile.toFile().writeText("Hello, cruel world")
      verify(fileChangeSchemaPublisher, timeout(3000)).refreshAllSources()
   }

   @Test
   fun `file watcher detects new file created`() {
      val (fileChangeSchemaPublisher: FileChangeSchemaPublisher, watcher) = newWatcher()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(fileChangeSchemaPublisher, timeout(3000)).refreshAllSources()
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (fileChangeSchemaPublisher: FileChangeSchemaPublisher, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      // Need to sleep to let the filewatch register for new events there
      Thread.sleep(500)
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(fileChangeSchemaPublisher, timeout(3000).atLeast(1)).refreshAllSources()
   }

   @Test
   fun `handles new nested folder`() {
      val (fileChangeSchemaPublisher: FileChangeSchemaPublisher, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      Thread.sleep(500)
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      Thread.sleep(500)
      // Need to sleep to let the filewatch register for new events there

      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(fileChangeSchemaPublisher, timeout(3000).atLeast(3)).refreshAllSources()
   }

   private fun newWatcher(): Pair<FileChangeSchemaPublisher, FilePoller> {
      val fileChangeSchemaPublisher: FileChangeSchemaPublisher = mock { }
      val watcher = FilePoller(
         FileSystemVersionedSourceLoader(folder.root.canonicalPath),
         1,
         fileChangeSchemaPublisher
      )
      // Wait a bit, to let the watcher get started before we do anything
      Thread.sleep(500)
      return fileChangeSchemaPublisher to watcher
   }

   // Merge conflict -- is this still needed?
//      @After
//   fun stopPoller() {
//      poller.close()
//   }
}
