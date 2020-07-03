package io.vyne

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.expect
import io.vyne.models.OperationResult
import io.vyne.models.json.addKeyValuePair
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.LineageGraph
import io.vyne.query.LineageGraphSerializationModule
import io.vyne.query.RemoteCall
import io.vyne.query.ResultMode
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class LineageGraphSerializationTest {
   val schema = """
         type Pet {
            id : Int
         }
         service PetService {
            @HttpOperation(method = "GET" , url = "http://petstore.swagger.io/api/pets/{id}")
            operation findPetById(  id : Int ) : Pet
         }
      """.trimIndent()

   @Test
   fun canSerializeLineage() {
      val (vyne, stubService) = testVyne(schema)
      vyne.addKeyValuePair("lang.taxi.Int", 100)
      stubService.addResponse("findPetById") { _, params ->
         vyne.typedValue("Pet", mapOf("id" to 100), source = OperationResult(
            mock { },
            params.map { (parameter, instance) ->
               OperationResult.OperationParam(parameter.name ?: "unknown", instance)
            }
         ))

      }

      val result = vyne.query(resultMode = ResultMode.VERBOSE).find("Pet")
      val lightweightResult = LightweightResult(result.resultMap)

      val json = jacksonObjectMapper()
         .registerModule(LineageGraphSerializationModule())
         .writerWithDefaultPrettyPrinter()
         .writeValueAsString(lightweightResult)

      val expectedJson = """{
  "resultMap" : {
    "Pet" : {
      "typeName" : "Pet",
      "value" : {
        "id" : {
          "typeName" : "lang.taxi.Int",
          "value" : 100,
          "source" : {
            "dataSourceIndex" : 0
          }
        }
      },
      "source" : {
        "dataSourceIndex" : 0
      }
    }
  },
  "lineageGraph" : {
    "1" : {
      "name" : "Provided"
    },
    "0" : {
      "inputs" : [ {
        "parameterName" : "id",
        "value" : {
          "value" : 100,
          "source" : {
            "dataSourceIndex" : 1
          },
          "typeName" : "lang.taxi.Int"
        }
      } ],
      "name" : "Operation result",
      "remoteCall" : {
        "service" : null,
        "addresss" : null,
        "operation" : null,
        "responseTypeName" : null,
        "method" : null,
        "requestBody" : null,
        "resultCode" : 0,
        "durationMs" : 0,
        "response" : null,
        "operationQualifiedName" : null
      }
    }
  }
}"""

      JSONAssert.assertEquals(expectedJson,json, true)
   }
}

// This class exists simply to reduce the noise in the seiralized output
private data class LightweightResult(val resultMap: Map<String, Any?>) {
   val lineageGraph:LineageGraph = LineageGraph
   // HACK : Put this last, so that other stuff is serialized first


}
