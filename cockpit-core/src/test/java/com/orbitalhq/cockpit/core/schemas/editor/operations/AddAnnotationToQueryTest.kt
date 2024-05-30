package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.annotations.http.HttpOperations
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
import org.junit.jupiter.api.Test

class AddOrRemoveAnnotationFromQueryTest : BaseSchemaEditOperationTest() {

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
         AddOrRemoveHttpEndpointAnnotation(
            queryQualifiedName = "MyQuery".fqn(),
            method = HttpOperations.HttpMethod.GET,
            path = "/api/q/somePath",
            operation = AddOrRemoveAnnotationFromQuery.Operation.Add,
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
   fun `can remove HttpEndpoint annotation from query`() {
      stubPackageApiToReturnPackage("""
         model Film {}
         type Title inherits String


      """.trimIndent())
      val result = applyEdit(
         "query.taxi",
         """
            import taxi.http.HttpOperation

            @HttpOperation(method = "GET", url = "/api/q/somePath")
            query MyQuery {
               find { Film[] } as {
                  title : Title
               }[]
            }
         """.trimIndent(),
         AddOrRemoveHttpEndpointAnnotation(
            queryQualifiedName = "MyQuery".fqn(),
            method = HttpOperations.HttpMethod.GET,
            operation = AddOrRemoveAnnotationFromQuery.Operation.Remove,
            path = "/api/q/somePath"
         )
      )
      val query = result.queries.single()
      query.httpEndpoint.shouldBeNull()
      query.sources.single().content.shouldBe("""import taxi.http.HttpOperation

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
         AddOrRemoveWebsocketEndpointAnnotation(
            queryQualifiedName = "MyQuery".fqn(),
            path = "/api/q/somePath",
            operation = AddOrRemoveAnnotationFromQuery.Operation.Add,
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
   fun `can remove Websocket annotation from query`() {
      stubPackageApiToReturnPackage("""
         model Film {}
         type Title inherits String


      """.trimIndent())
      val result = applyEdit(
         "query.taxi",
         """
            import taxi.http.WebsocketOperation

            @WebsocketOperation(path = "/api/q/somePath")
            query MyQuery {
               find { Film[] } as {
                  title : Title
               }[]
            }
         """.trimIndent(),
         AddOrRemoveWebsocketEndpointAnnotation(
            queryQualifiedName = "MyQuery".fqn(),
            path = "/api/q/somePath",
            operation = AddOrRemoveAnnotationFromQuery.Operation.Remove,
         )
      )
      val query = result.queries.single()
      query.websocketOperation.shouldBeNull()
      query.sources.single().content.shouldBe("""import taxi.http.WebsocketOperation

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
         AddOrRemoveHttpEndpointAnnotation(
            queryQualifiedName = "foo.bar.MyQuery".fqn(),
            method = HttpOperations.HttpMethod.GET,
            path = "/api/q/somePath",
            operation = AddOrRemoveAnnotationFromQuery.Operation.Add,
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
