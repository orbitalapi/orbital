package io.vyne.schemaStore

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

@ExperimentalPathApi
class FileSystemSchemaRepositoryTest {

   @Rule
   @JvmField
   val projectHome = TemporaryFolder()

   @Test
   fun `can write new type to a local project`() {
      copyProject("sample-project")
      val repo = FileSystemSchemaRepository(projectHome.root.toPath())

      val changed = VersionedSource("foo/bar/hello.taxi", "", "type Hello inherits String")
      repo.writeSource(changed)

      // Note - should've been wrin under src, since that's the sourceDir configured in the taxi project
      val writtenSource = projectHome.root.toPath().resolve("src/foo/bar/hello.taxi")
         .readText()
      writtenSource.should.equal("type Hello inherits String")
   }


   private fun copyProject(path: String) {
      val testProject = File(Resources.getResource(path).toURI())
      FileUtils.copyDirectory(testProject, projectHome.root)
   }
}
