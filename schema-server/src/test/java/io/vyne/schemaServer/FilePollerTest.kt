package io.vyne.schemaServer

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
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
      val (localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge, watcher) = newWatcher()

      createdFile.toFile().writeText("Hello, cruel world")
      verify(localFileSchemaPublisherBridge, timeout(3000)).rebuildSourceList()
   }

   @Test
   fun `file watcher detects new file created`() {
      val (localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge, watcher) = newWatcher()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(localFileSchemaPublisherBridge, timeout(3000)).rebuildSourceList()
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      // Need to sleep to let the filewatch register for new events there
      Thread.sleep(500)
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(localFileSchemaPublisherBridge, timeout(3000).atLeast(1)).rebuildSourceList()
   }

   @Test
   fun `handles new nested folder`() {
      val (localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      Thread.sleep(500)
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      Thread.sleep(500)
      // Need to sleep to let the filewatch register for new events there

      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(localFileSchemaPublisherBridge, timeout(3000).atLeast(3)).rebuildSourceList()
   }

   private fun newWatcher(): Pair<LocalFileSchemaPublisherBridge, FilePoller> {
      val localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge = mock { }
      val watcher = FilePoller(
         FileSystemVersionedSourceLoader(folder.root.canonicalPath),
         1,
         localFileSchemaPublisherBridge
      )
      // Wait a bit, to let the watcher get started before we do anything
      Thread.sleep(500)
      return localFileSchemaPublisherBridge to watcher
   }

   // Merge conflict -- is this still needed?
//      @After
//   fun stopPoller() {
//      poller.close()
//   }
}
