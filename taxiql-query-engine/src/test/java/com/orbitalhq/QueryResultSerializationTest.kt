package com.orbitalhq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.VyneQueryStatistics
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
      val clientInstnace = vyne.parseJsonModel(
         "Client", """{
          | "clientId" : "123",
          | "name" : "Jimmy",
          | "isicCode" : "isic"
          | }
      """.trimMargin()
      )
      val queryId = UUID.randomUUID().toString()
      val result = QueryResult(
         queryId = queryId,
         results = flow { emit(clientInstnace) },
         querySpec = QuerySpecTypeNode(clientType),
         isFullyResolved = true
      )

      val expectedJson = """
         {
           "queryResponseId" : "${result.queryResponseId}",
             "searchedTypeName" : {
               "fullyQualifiedName" : "Client",
               "parameters" : [ ],
               "name" : "Client",
               "namespace" : "",
               "parameterizedName" : "Client",
               "longDisplayName" : "Client",
               "shortDisplayName" : "Client"
             },
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
      val clientInstnace = vyne.parseJsonModel(
         "Client", """{
          | "clientId" : "123",
          | "name" : "Jimmy",
          | "isicCode" : "isic"
          | }
      """.trimMargin()
      )
      val queryId = UUID.randomUUID().toString()
      val collection = TypedCollection.arrayOf(clientType, listOf(clientInstnace))
      val result = QueryResult(
         queryId = queryId,
         results = flow { emit(clientInstnace) },
         querySpec = QuerySpecTypeNode(clientType),
         isFullyResolved = true
      )

      val expected = """
{
  "queryResponseId" : "${result.queryResponseId}",
    "searchedTypeName" : {
      "fullyQualifiedName" : "Client",
      "parameters" : [ ],
      "name" : "Client",
      "namespace" : "",
      "parameterizedName" : "Client",
      "longDisplayName" : "Client",
      "shortDisplayName" : "Client"
    },
   "responseStatus" : "COMPLETED",
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
