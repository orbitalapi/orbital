package io.vyne.schemaServer

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.timeout
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

// I've tried to make this test as safe as possible to avoid timing
// related issues.  FileWatchers run on their own threads, and
// the scheduling is very OS dependant, so we could start to see intermittent
// failures.  If so, revisit these tests.
class FileWatcherTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var watcher: FileWatcher
   lateinit var watcherThread: Thread

   @After
   fun tearDown() {
      if (this::watcher.isInitialized) {
         watcher.cancelWatch()
         watcher.destroy()
      }
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

      verify(compilerService, timeout(30000)).recompile(any())
   }

   @Test
   fun `file watcher detects new file created`() {
      val (compilerService: CompilerService, watcher) = newWatcher()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(compilerService, timeout(30000).atLeast(1)).recompile(any())
   }

   @Test
   fun `file watcher detects new directory created`() {
      val (compilerService: CompilerService, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      expectRecompilationTriggered(compilerService)

      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")
      expectRecompilationTriggered(compilerService)
   }

   @Test
   fun `handles new nested folder`() {
      val (compilerService: CompilerService, watcher) = newWatcher()

      val newDir = folder.newFolder("newDir").toPath()
      expectRecompilationTriggered(compilerService)

      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      expectRecompilationTriggered(compilerService)

      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")
      expectRecompilationTriggered(compilerService)
   }

   private fun expectRecompilationTriggered(compilerService: CompilerService) {
      verify(compilerService,  timeout(30000)).recompile(any())
      reset(compilerService)
   }

   private fun newWatcher(): Pair<CompilerService, FileWatcher> {
      val compilerService: CompilerService = mock { }
      val watcher = FileWatcher(
         FileSystemVersionedSourceLoader(folder.root.canonicalPath),
         0,
         incrementVersionOnRecompile = false,
         compilerService = compilerService
      )
      watcherThread = Thread { watcher.watch() }
      watcherThread.start()

      //Yeah .. i know this is bad but checking for active in another thread doesnt work
      //on mac ! .. no idea why
      for (i in 1..5) {
         if (watcher.isActive) {
            return compilerService to watcher
         }
         Thread.sleep(5000)
      }
      // Wait a bit, to let the watcher get started before we do anything
      return compilerService to watcher
   }
}
