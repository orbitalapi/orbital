package com.orbitalhq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.parseJsonModel
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QuerySpecTypeNode
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.QueryErrorEvent
import kotlinx.coroutines.flow.flow
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import reactor.core.publisher.Flux
import java.util.UUID


class QueryResultSerializationTest {

   val taxiDef = """
 type Client {
   clientId : ClientId inherits String
   name : ClientName inherits String
   isicCode : IsicCode inherits String
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
         isFullyResolved = true,
         schema = vyne.schema,
         responseType = clientInstnace.type,
         errors = Flux.just(QueryErrorEvent(queryId, StreamErrorMessage.fromException(IllegalArgumentException("error"), "Client")))
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
               "responseType" : "Client",
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
         isFullyResolved = true,
         schema = vyne.schema,
         responseType = clientInstnace.type,
          errors = Flux.empty()
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
      "responseType" : "Client",
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

