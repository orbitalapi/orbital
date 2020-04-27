package io.vyne

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.models.TypedCollection
import io.vyne.models.json.parseJsonModel
import io.vyne.query.QueryResult
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.ResultMode
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class QueryResultSerializationTest {

   val taxiDef = """
 type Client {
   clientId : ClientId as String
   name : ClientName as String
   isicCode : IsicCode as String
}
   """.trimIndent()

   @Test
   fun given_queryResultWithObjectResponse_then_itIsSerializedAsTypeNamedInstance() {
      val (vyne, _) = testVyne(taxiDef)
      val clientType = vyne.type("Client")
      val clientInstnace = vyne.parseJsonModel("Client", """{
          | "clientId" : "123",
          | "name" : "Jimmy",
          | "isicCode" : "isic"
          | }
      """.trimMargin())
      val result = QueryResult(
         results = mapOf(
            QuerySpecTypeNode(clientType) to clientInstnace
         ), resultMode = ResultMode.SIMPLE
      )


      val expectedJson = """{
  "results": {
    "Client": {
      "clientId": "123",
      "name": "Jimmy",
      "isicCode": "isic"
    }
  },
  "unmatchedNodes": [],
  "path": null,
  "queryResponseId": "${result.queryResponseId}",
  "resultMode": "SIMPLE",
  "duration": null,
  "fullyResolved": true,
  "remoteCalls": [],
  "timings": {},
  "vyneCost": 0
}"""
      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)
      JSONAssert.assertEquals(expectedJson, json, true)
   }

   @Test
   fun given_queryResultWithCollection_then_itIsSerializedCorrectly() {
      val (vyne, _) = testVyne(taxiDef)
      val clientType = vyne.type("Client")
      val clientInstnace = vyne.parseJsonModel("Client", """{
          | "clientId" : "123",
          | "name" : "Jimmy",
          | "isicCode" : "isic"
          | }
      """.trimMargin())
      val collection = TypedCollection(clientType, listOf(clientInstnace))
      val result = QueryResult(
         results = mapOf(
            QuerySpecTypeNode(clientType) to collection
         ), resultMode = ResultMode.SIMPLE
      )
      val expected = """
 {
  "results" : {
    "Client" : [ {
      "clientId" : "123",
      "name" : "Jimmy",
      "isicCode" : "isic"
    } ]
  },
  "unmatchedNodes" : [ ],
  "path" : null,
  "queryResponseId" : "${result.queryResponseId}",
  "resultMode" : "SIMPLE",
  "duration" : null,
  "fullyResolved" : true,
  "remoteCalls" : [ ],
  "timings" : { },
  "vyneCost" : 0
}
      """.trimIndent()

      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)
      JSONAssert.assertEquals(expected, json, true)
   }
}
