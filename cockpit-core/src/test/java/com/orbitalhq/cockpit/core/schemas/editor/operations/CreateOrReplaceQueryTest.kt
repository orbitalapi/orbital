package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.VersionedSource
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test


class CreateOrReplaceQueryTest : BaseSchemaEditOperationTest() {


   @Test
   fun `can create a new query`() {
      stubPackageApiToReturnPackage(
         """
         model Film {}
         type Title inherits String


      """.trimIndent()
      )

      val result = applyEdit(
         "query.taxi",
         "",
         CreateOrReplaceQuery(
            listOf(
               VersionedSource.unversioned("myQuery.taxi",
                  """find { Film[] } as {
   title : Title
}[]
            """.trimIndent()
               )
            )
         )
      )
      result.queries.shouldHaveSize(1)
      val source = result.sourcePackage.source("myQuery.taxi")
      // The query block should've been prepended
      source.content.shouldBe("""query myQuery {
   find { Film[] } as {
      title : Title
   }[]
}""")
   }
}
