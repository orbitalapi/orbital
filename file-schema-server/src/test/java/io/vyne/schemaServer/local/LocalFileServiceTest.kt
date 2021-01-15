package io.vyne.schemaServer.local

import com.winterbe.expekt.should
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class LocalFileServiceTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test(expected = IllegalPathException::class)
   fun `file with bad extension causes exception`() {
      fileService().writeContents(path("foo.exe"), "hello")
   }

   @Test
   fun writesContentsToFile() {
      fileService().writeContents(path("foo.taxi"), "hello")

      val file = path("foo.taxi").toFile()
      file.exists().should.be.`true`
      file.readText().should.equal("hello")
   }
   @Test
   fun writesContentsToExistingFileOverwritesContents() {
      fileService().writeContents(path("foo.taxi"), "hello")
      fileService().writeContents(path("foo.taxi"), "world")

      val file = path("foo.taxi").toFile()
      file.exists().should.be.`true`
      file.readText().should.equal("world")
   }

   private fun path(path: String): Path {
      return folder.root.toPath().resolve(path)
   }

   @Test(expected = IllegalPathException::class)
   fun `file with path that is above root causes exception`() {
      fileService().writeContents(path("../foo.taxi"), "hello")
   }

   @Test
   fun `can create file in nested directory`() {
      fileService().writeContents(path("src/foo/bar/baz/foo.taxi"), "hello")
      val file = path("src/foo/bar/baz/foo.taxi").toFile()
      file.exists().should.be.`true`
      file.readText().should.equal("hello")

      // Now that the directory exists, add another file, to ensure the mkdirs() calls don't fail
      fileService().writeContents(path("src/foo/bar/baz/floop.taxi"), "hello")
      val floopFile = path("src/foo/bar/baz/floop.taxi").toFile()
      floopFile.exists().should.be.`true`
      floopFile.readText().should.equal("hello")
   }

   private fun fileService(extensions: List<String> = listOf("taxi")): LocalFileService {
      return LocalFileService(folder.root.toPath(), extensions)
   }
}
