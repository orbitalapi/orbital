package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.annotations.http.HttpOperations
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.shouldBe
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
import org.junit.jupiter.api.Test

class AddAnnotationToQueryTest : BaseSchemaEditOperationTest() {

   @Test
   fun `can add HttpEndpoint annotation to query`() {
      stubPackageApiToReturnPackage("""
         model Film {}
         type Title inherits String


      """.trimIndent())
      val result = applyEdit(
         "query.taxi",
         """
            query MyQuery {
               find { Film[] } as {
                  title : Title
               }[]
            }
         """.trimIndent(),
         AddHttpEndpointToQuery(
            queryQualifiedName = "MyQuery".fqn(),
            method = HttpOperations.HttpMethod.GET,
            path = "/api/q/somePath"
         )
      )
      val query = result.queries.single()
      query.httpEndpoint.shouldBe(HttpOperation(method = "GET", url ="/api/q/somePath"))
      query.sources.single().content.shouldBe("""import taxi.http.HttpOperation

@HttpOperation(method = "GET", url = "/api/q/somePath")
query MyQuery {
   find { Film[] } as {
      title : Title
   }[]
}""")
   }

   @Test
   fun `can add Websocket annotation to query`() {
      stubPackageApiToReturnPackage("""
         model Film {}
         type Title inherits String


      """.trimIndent())
      val result = applyEdit(
         "query.taxi",
         """
            query MyQuery {
               find { Film[] } as {
                  title : Title
               }[]
            }
         """.trimIndent(),
         AddWebsocketEndpointToQuery(
            queryQualifiedName = "MyQuery".fqn(),
            path = "/api/q/somePath"
         )
      )
      val query = result.queries.single()
      query.websocketOperation.shouldBe(WebsocketOperation(path ="/api/q/somePath"))
      query.sources.single().content.shouldBe("""import taxi.http.WebsocketOperation

@WebsocketOperation(path = "/api/q/somePath")
query MyQuery {
   find { Film[] } as {
      title : Title
   }[]
}""")
   }

   @Test
   fun `when adding HttpEndpoint existing imports are respected`() {
      stubPackageApiToReturnPackage("""
         namespace films
         model Film {}
         type Title inherits String


      """.trimIndent())
      val result = applyEdit(
         "query.taxi",
         """
            import films.Film
            import films.Title

            namespace foo.bar

            query MyQuery {
               find { Film[] } as {
                  title : Title
               }[]
            }
         """.trimIndent(),
         AddHttpEndpointToQuery(
            queryQualifiedName = "foo.bar.MyQuery".fqn(),
            method = HttpOperations.HttpMethod.GET,
            path = "/api/q/somePath"
         )
      )
      // Note - this isn't nicely formatted, maybe we can improve that later?
      result.sourcePackage.sources.single().content.shouldBe("""import films.Film
import films.Title
import taxi.http.HttpOperation


namespace foo.bar
@HttpOperation(method = "GET", url = "/api/q/somePath")
query MyQuery {
   find { Film[] } as {
      title : Title
   }[]
}""")
   }
}
