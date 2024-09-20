package com.orbitalhq.query.runtime.core.gateway

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.schemas.fqn
import com.orbitalhq.testVyne
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OpenApiSpecServiceTest {

   @Test
   fun `generates an oas for a get query`() {
      val (vyne, stub) = testVyne(
         """
         model Person {
            name : Name inherits String
         }
         @taxi.http.HttpOperation(url="/api/q/myQuery", method = "GET")
         query MySavedQuery {
            find { Person }
         }
      """.trimIndent()
      )

      val oasService = OpenApiSpecService(mock { }, mock { })
      val oasYaml = oasService.getApiSpecForQuery(vyne.schema, "MySavedQuery".fqn())
      val expected = """openapi: 3.0.1
info:
  title: MySavedQuery
  version: 1.0.0
paths:
  /api/q/myQuery:
    get:
      parameters: []
      responses:
        "200":
          content:
            application/json:
              schema:
                ${"$"}ref: "#/components/schemas/Person"
components:
  schemas:
    Person:
      type: object
      properties:
        name:
          type: string
          x-taxi-type:
            name: Name
            create: false
"""
      oasYaml.shouldBe(expected)
   }

   @Test
   fun `generates an oas for a mutation`() {
      val (vyne, stub) = testVyne(
         """
         model Person {
            name : Name inherits String
         }
         model UpdatedPerson {
            id : Id inherits String
            calledBy : Name
         }

         service MyDbService {
            write operation updatePerson(Person)
         }
         @taxi.http.HttpOperation(url="/api/q/myQuery", method = "GET")
         query MySavedQuery {
            find { Person }
            call MyDbService::updatePerson
         }
      """.trimIndent()
      )
      val oasService = OpenApiSpecService(mock { }, mock { })
      val oasYaml = oasService.getApiSpecForQuery(vyne.schema, "MySavedQuery".fqn())
      oasYaml.shouldBe(
         """openapi: 3.0.1
info:
  title: MySavedQuery
  version: 1.0.0
paths:
  /api/q/myQuery:
    get:
      parameters: []
      responses:
        "200":
          description: Success
components:
  schemas: {}
"""
      )
   }
}
