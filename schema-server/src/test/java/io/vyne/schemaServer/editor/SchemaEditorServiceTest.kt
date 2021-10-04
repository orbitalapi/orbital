package io.vyne.schemaServer.editor

import com.winterbe.expekt.should
import io.vyne.schemaServer.file.deployProject
import io.vyne.schemas.fqn
import io.vyne.utils.withoutWhitespace
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class SchemaEditorServiceTest {

   @Rule
   @JvmField
   val projectHome = TemporaryFolder()

   @Test
   fun `can submit annotations to type`() {
      val projectPath = projectHome.deployProject("sample-project")
      val editorRepository = ApiEditorRepository.forPath(projectPath)

      val editor = SchemaEditorService(editorRepository)
      editor.updateAnnotationsOnType(
         "com.foo.Bar", UpdateTypeAnnotationRequest(
            listOf("Documented".fqn(), "com.foo.Sensitive".fqn())
         )
      )

      val expectedFilePath = projectPath.resolve("src/com/foo/Bar.annotations.taxi")
      Files.exists(expectedFilePath).should.be.`true`

      val contents = expectedFilePath.toFile().readText()
      contents.withoutWhitespace().should.equal(
"""namespace com.foo

// This code is generated, and will be automatically updated
@Documented
@com.foo.Sensitive
type extension Bar""".withoutWhitespace()
      )

   }

}
