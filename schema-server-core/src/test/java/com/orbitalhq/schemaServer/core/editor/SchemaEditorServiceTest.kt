package com.orbitalhq.schemaServer.core.editor

import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageSourceName
import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schema.publisher.loaders.Changeset
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import com.orbitalhq.schemaServer.editor.SaveQueryRequest
import com.orbitalhq.schemaServer.editor.UpdateTypeAnnotationRequest
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.withoutWhitespace
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.time.Duration
import kotlin.io.path.readText

class SchemaEditorServiceTest {

   @Rule
   @JvmField
   val projectHome = TemporaryFolder()

   @Test
   fun `can submit annotations to type`() {
      val projectPath = projectHome.deployProject("sample-project")

      val repositoryManager =
         ReactiveRepositoryManager.testWithFileRepo(projectPath, isEditable = true)
      val schema = TaxiSchema.compiled("namespace com.foo { model Bar{} }").second
      val editor = SchemaEditorService(repositoryManager, SimpleSchemaStore(SchemaSet.from(schema, 0)))
      editor.updateAnnotationsOnType(
         "com.foo.Bar", UpdateTypeAnnotationRequest(
            listOf(Metadata("Documented".fqn()), Metadata("com.foo.Sensitive".fqn())),
            Changeset(
               "",
               isActive = true,
               isDefault = true,
               packageIdentifier = PackageIdentifier.fromId("taxi/sample/0.1.0")
            )
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
      val repositoryManager =
         ReactiveRepositoryManager.testWithFileRepo(projectPath, isEditable = true)

      val schema = TaxiSchema.compiled("namespace com.foo { enum Bar{} }").second
      val editor = SchemaEditorService(repositoryManager, SimpleSchemaStore(SchemaSet.from(schema, 0)))
      editor.updateAnnotationsOnType(
         "com.foo.Bar", UpdateTypeAnnotationRequest(
            listOf(Metadata("Documented".fqn()), Metadata("com.foo.Sensitive".fqn())),
            Changeset(
               "",
               isActive = true,
               isDefault = true,
               packageIdentifier = PackageIdentifier.fromId("taxi/sample/0.1.0")
            )
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


   @Test
   fun `saving a query adds query annotation`() {
      val projectPath = projectHome.deployProject("sample-project")
      val repositoryManager =
         ReactiveRepositoryManager.testWithFileRepo(projectPath, isEditable = true)
      val schema = TaxiSchema.compiled("namespace com.foo { model Person{} }").second
      val editor = SchemaEditorService(repositoryManager, SimpleSchemaStore(SchemaSet.from(schema, 0)))

      val saved = editor.saveQuery(
         SaveQueryRequest(
            VersionedSource(
               PackageSourceName(
                  PackageIdentifier("taxi", "sample", "1.0.0"),
                  "MyQuery.taxi"
               ),
               content = """find { Person }"""
            )
         )
      ).block()!!
      val expected = """query MyQuery {
   find { Person }
}""".trimIndent()
      saved.sources.single().content.shouldBe(expected)
      val savedSource = projectPath.resolve("src/MyQuery.taxi").readText()
      savedSource.shouldBe(expected)
   }

}
