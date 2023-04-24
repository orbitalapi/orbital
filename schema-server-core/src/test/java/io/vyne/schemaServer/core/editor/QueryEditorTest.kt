package io.vyne.schemaServer.core.editor

import io.kotest.matchers.shouldBe
import io.vyne.PackageIdentifier
import io.vyne.PackageSourceName
import io.vyne.VersionedSource
import org.junit.Test

class QueryEditorTest {
   private val packageSourceName = PackageSourceName(PackageIdentifier("com.foo", "Films", "1.0.0"), "MyQuery.taxi")

   @Test
   fun `if query declaration is missing then it is added`() {

      val updated = QueryEditor.prependQueryBlockIfMissing(
         VersionedSource(packageSourceName, """find { Person }""")
      )
      updated.content.shouldBe(
         """query MyQuery {
         |   find { Person }
         |}
      """.trimMargin()
      )
   }

   @Test
   fun `if query declaration is present then the query isnt changed`() {

      val querySource = """query MyQuery { find { Person } }"""
      val updated = QueryEditor.prependQueryBlockIfMissing(
         VersionedSource(packageSourceName, querySource)
      )
      updated.content.shouldBe(querySource)
   }
}
