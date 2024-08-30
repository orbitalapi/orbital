package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.Vyne
import com.orbitalhq.errors.ErrorType
import com.orbitalhq.errors.QueryErrors
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import lang.taxi.annotations.HttpService

class HttpErrorResponseTest : DescribeSpec({
   describe("Converting exceptions to http responses") {
      it("should get response code using an annotation") {
         val (status, body) = errorSchema(
            """
@ResponseCode(401)
model NotAuthorizedError inherits Error {
  message: ErrorMessage
}
      """.trimIndent()
         ).getErrorCodeAndPayload("NotAuthorizedError", """{ "message" : "Nope" }""")
         status.value().shouldBe(401)
         body.shouldBe("Nope")
      }

      it("should use default response code if not provided") {
         val (status, body) = errorSchema(
            """
model NotAuthorizedError inherits Error {
  message: ErrorMessage
}
      """.trimIndent()
         ).getErrorCodeAndPayload("NotAuthorizedError", """{ "message" : "Nope" }""")
         status.value().shouldBe(400)
      }

      it("should serialize a field annotated as response body") {
         val (status, body) = errorSchema(
            """
model NotAuthorizedError inherits Error {
  @ResponseBody
  error: {
      errorCode: Int
      message: String
   }
}
      """.trimIndent()
         ).getErrorCodeAndPayload("NotAuthorizedError", """{ "error" : { "errorCode" : 123, "message" : "nope" } }""")
         body.shouldBe("""{"errorCode":123,"message":"nope"}""")
      }

      it("should serialize response body") {
         val (status, body) = errorSchema(
            """
@ResponseBody
model NotAuthorizedError inherits Error {
      errorCode: Int
      message: String
}
      """.trimIndent()
         ).getErrorCodeAndPayload("NotAuthorizedError", """{ "errorCode" : 123, "message" : "nope" } """)
         body.shouldBe("""{"errorCode":123,"message":"nope"}""")
      }
   }


})

private fun errorSchema(
   schema: String, imports: String = """import taxi.http.ResponseCode
import taxi.http.ResponseBody
"""
): Vyne {
   return testVyne(
      """$imports
         |
         |$schema
      """.trimMargin(), HttpService.asTaxi(), ErrorType.ErrorTypeDefinition
   ).first
}

private fun Vyne.getErrorCodeAndPayload(typeName: String, errorJson: String): HttpErrorResponse.OrbitalQueryExceptionHttpInfo {
   val error = this.parseJson(typeName, errorJson)
   val exception = QueryErrors.toException(error, this.schema, mapOf())
   return HttpErrorResponse.getErrorCodeAndPayload(exception)
}
