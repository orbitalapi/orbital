package io.vyne.schemaServer

import com.nhaarman.mockito_kotlin.any
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
      val compilerService = startPoller()

      createdFile.toFile().writeText("Hello, cruel world")
      verify(compilerService, timeout(3000)).recompile(eq(folder.root.canonicalPath.toString()), any())
   }

   @Test
   fun `file watcher detects new file created`() {
      val compilerService = startPoller()
      val createdFile = folder.root.toPath().resolve("hello.taxi")
      createdFile.toFile().writeText("Hello, world")

      verify(compilerService, timeout(3000)).recompile(eq(folder.root.canonicalPath.toString()), any())
   }

   @Test
   fun `file watcher detects new directory created`() {
      val compilerService = startPoller()

      val newDir = folder.newFolder("newDir").toPath()
      // Need to sleep to let the filewatch register for new events there
      Thread.sleep(500)
      newDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(compilerService, timeout(3000).atLeast(1)).recompile(eq(folder.root.canonicalPath.toString()), any())
   }

   @Test
   fun `handles new nested folder`() {
      val compilerService = startPoller()

      val newDir = folder.newFolder("newDir").toPath()
      Thread.sleep(500)
      val nestedDir = newDir.resolve("nested/")
      nestedDir.toFile().mkdirs()
      Thread.sleep(500)
      // Need to sleep to let the filewatch register for new events there

      nestedDir.resolve("hello.taxi").toFile().writeText("Hello, world")

      verify(compilerService, timeout(3000).atLeast(3)).recompile(eq(folder.root.canonicalPath.toString()), any())
   }

   private fun startPoller(): CompilerService {
      val compilerService: CompilerService = mock()
      poller = FilePoller(
         FileSystemVersionedSourceLoader(folder.root.canonicalPath),
         1,
         incrementVersionOnRecompile = false,
         compilerService = compilerService
      )
      // Wait a bit, to let the watcher get started before we do anything
      Thread.sleep(500)
      return compilerService
   }

   @After
   fun stopPoller() {
      poller.close()
   }
}
