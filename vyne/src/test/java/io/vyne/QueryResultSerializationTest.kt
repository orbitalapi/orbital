package io.vyne

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJsonModel
import io.vyne.query.QueryResult
import io.vyne.query.QuerySpecTypeNode
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*


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
      val queryId = UUID.randomUUID().toString()
      val result = QueryResult(
         queryId = queryId,
         results = flow { emit(clientInstnace) },
         querySpec = QuerySpecTypeNode(clientType)
      )

      val expectedJson = """
         {
           "unmatchedNodes" : [ ],
           "queryResponseId" : "${result.queryResponseId}",
           "truncated" : false,
           "anonymousTypes" : [ ],
            "responseStatus" : "COMPLETED",
           "vyneCost" : 0,
           "timings" : { },
           "remoteCalls" : [ ],
           "fullyResolved" : true,
           "queryId": $queryId
         }
      """.trimIndent()
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
      val queryId = UUID.randomUUID().toString()
      val collection = TypedCollection.arrayOf(clientType, listOf(clientInstnace))
      val result = QueryResult(
         queryId = queryId,
         results = flow { emit(clientInstnace) },
         querySpec = QuerySpecTypeNode(clientType)
      )

      val expected = """
{
  "unmatchedNodes" : [ ],
  "queryResponseId" : "${result.queryResponseId}",
   "responseStatus" : "COMPLETED",
  "truncated" : false,
  "anonymousTypes" : [ ],
  "remoteCalls" : [ ],
  "timings" : { },
  "vyneCost" : 0,
  "fullyResolved" : true,
  "queryId":   $queryId
}
      """.trimIndent()

      val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)
      JSONAssert.assertEquals(expected, json, true)
   }
}

