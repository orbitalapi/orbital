package io.vyne.schemaServer.core.editor

import com.winterbe.expekt.should
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaServer.core.file.deployProject
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.editor.UpdateTypeAnnotationRequest
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.Metadata
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.time.Duration

class SchemaEditorServiceTest {

   @Rule
   @JvmField
   val projectHome = TemporaryFolder()

   @Test
   fun `can submit annotations to type`() {
      val projectPath = projectHome.deployProject("sample-project")

      val repositoryManager = ReactiveRepositoryManager.testWithFileRepo(projectPath, isEditable = true)
      val schema = TaxiSchema.compiled("namespace com.foo { model Bar{} }").second
      val editor = SchemaEditorService(repositoryManager, SimpleSchemaStore(SchemaSet.from(schema, 0)))
      editor.updateAnnotationsOnType(
         "com.foo.Bar", UpdateTypeAnnotationRequest(
            listOf(Metadata("Documented".fqn()), Metadata("com.foo.Sensitive".fqn()))
         )
      ).block(Duration.ofMillis(1000))

      val expectedFilePath = projectPath.resolve("src/com/foo/Bar.annotations.taxi")
      Files.exists(expectedFilePath).should.be.`true`

      val contents = expectedFilePath.toFile().readText()
      contents.withoutWhitespace().should.equal(
         """namespace com.foo

// This code is generated, and will be automatically updated
@Documented
@com.foo.Sensitive
type extension Bar {}""".withoutWhitespace()
      )

   }

   @Test
   fun `can submit annotations to enum`() {
      val projectPath = projectHome.deployProject("sample-project")
      val repositoryManager = ReactiveRepositoryManager.testWithFileRepo(projectPath, isEditable = true)

      val schema = TaxiSchema.compiled("namespace com.foo { enum Bar{} }").second
      val editor = SchemaEditorService(repositoryManager, SimpleSchemaStore(SchemaSet.from(schema, 0)))
      editor.updateAnnotationsOnType(
         "com.foo.Bar", UpdateTypeAnnotationRequest(
            listOf(Metadata("Documented".fqn()), Metadata("com.foo.Sensitive".fqn()))
         )
      ).block()

      val expectedFilePath = projectPath.resolve("src/com/foo/Bar.annotations.taxi")
      Files.exists(expectedFilePath).should.be.`true`

      val contents = expectedFilePath.toFile().readText()
      contents.withoutWhitespace().should.equal(
         """namespace com.foo

// This code is generated, and will be automatically updated
@Documented
@com.foo.Sensitive
enum extension Bar {}""".withoutWhitespace()
      )
   }

}
