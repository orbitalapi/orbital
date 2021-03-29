package io.vyne.schemaServer

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.timeout
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

   lateinit var watcher: FilePoller
   lateinit var watcherThread: Thread

   @After
   fun tearDown() {
      if (this::watcherThread.isInitialized) {
         watcherThread.interrupt()
      }
   }

   @Test
   fun `file watcher detects changes to existing file`() {
      val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
      createdFile.toFile().writeText("Hello, world")
      val (compilerService: CompilerService, watcher) = newWatcher()

      createdFile.toFile().writeText("Hello, cruel world")
      verify(compilerService, timeout(3000)).recompile(any())
   }

   @Test
   fun `file watcher detects new file created`() {
      val (compilerService: CompilerService, watcher) = newWatcher()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(compilerService, timeout(3000)).recompile(any())
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (compilerService: CompilerService, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      // Need to sleep to let the filewatch register for new events there
      Thread.sleep(500)
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(compilerService, timeout(3000).times(2)).recompile(any())
   }

   @Test
   fun `handles new nested folder`() {
      val (compilerService: CompilerService, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      Thread.sleep(500)
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      Thread.sleep(500)
      // Need to sleep to let the filewatch register for new events there

      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(compilerService, timeout(3000).atLeast(3)).recompile(any())
   }

   private fun newWatcher(): Pair<CompilerService, FilePoller> {
      val compilerService: CompilerService = mock { }
      val watcher = FilePoller(
         folder.root.canonicalPath,
         1,
         incrementVersionOnRecompile = false,
         compilerService = compilerService
      )
      // Wait a bit, to let the watcher get started before we do anything
      Thread.sleep(500)
      return compilerService to watcher
   }
}
