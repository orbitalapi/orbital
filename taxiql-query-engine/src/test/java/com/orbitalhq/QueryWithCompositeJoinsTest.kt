package com.orbitalhq

import com.orbitalhq.models.json.tryParseJson
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class QueryWithCompositeJoinsTest {

   @Test
   fun `can use a composite value for querying`() {
      val (vyne, stub) = testVyne(
         """
         closed model Product {
           productId: ProductId inherits Int
           factor: FactorId inherits Int
         }

         closed model EsgScore {
           environmental: EScore inherits Decimal
         }

         type EsgScoreId inherits String by concat(ProductId, FactorId)

         service EsgService {
           operation getProducts():Product[]
           operation getEsgData(EsgScoreId):EsgScore
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getProducts",
         """[{ "productId" : 1, "factor" : 10 }, { "productId" : 2, "factor" : 20 }]"""
      )
      stub.addResponse("getEsgData", modifyDataSource = true) { _, args ->
         // echo back the input as the result, for asserting below
         listOf(vyne.tryParseJson("EsgScore", """{ "environmental" : ${args.single().second.toRawObject()} }"""))
      }
      val result = runBlocking {
         vyne.query(
            """find { Product[] }  as {
    productId: ProductId
    score: EScore
}[]"""
         ).rawObjects()

      }

      result.shouldContain(mapOf("productId" to 2, "score" to 220.toBigDecimal()))
      result.shouldContain(mapOf("productId" to 1, "score" to 110.toBigDecimal()))
   }


   @Test
   fun `can use a nested composite value for querying`() {
      val (vyne, stub) = testVyne(
         """
         closed model Product {
           productId: ProductId inherits Int
           factor: FactorId inherits Int
         }

         closed model EsgScore {
           rating: ESGRating inherits String
         }

         type TranslatedFactorId inherits String by concat(FactorId, "AAA")
         type EsgScoreId inherits String by concat(ProductId, TranslatedFactorId)

         service EsgService {
           operation getProducts():Product[]
           operation getEsgData(EsgScoreId):EsgScore
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getProducts",
         """[{ "productId" : 1, "factor" : 10 }, { "productId" : 2, "factor" : 20 }]"""
      )
      stub.addResponse("getEsgData", modifyDataSource = true) { _, args ->
         // echo back the input as the result, for asserting below
         listOf(vyne.tryParseJson("EsgScore", """{ "rating" : "${args.single().second.toRawObject()}" }"""))
      }
      val result = runBlocking {
         vyne.query(
            """find { Product[] }  as {
    productId: ProductId
    score: ESGRating
}[]"""
         ).rawObjects()
      }

      result.shouldContain(mapOf("productId" to 2, "score" to "220AAA"))
      result.shouldContain(mapOf("productId" to 1, "score" to "110AAA"))
   }

   @Test
   fun `when evaluating nested composite value order of parameters to expression doesnt matter`() {
      // This is the same test as above, but with the params reordered so that inputs don't come from the
      // graph - trying to prove that when navigating the expression, if the previous node is null (ie.,
      // there isn't a value because it's a constant) then the evaluation can still proceed
      val (vyne, stub) = testVyne(
         """
         closed model Product {
           productId: ProductId inherits Int
           factor: FactorId inherits Int
         }

         closed model EsgScore {
           rating: ESGRating inherits String
         }

         type TranslatedFactorId inherits String by concat("AAA",FactorId)
         type EsgScoreId inherits String by concat(TranslatedFactorId,ProductId)

         service EsgService {
           operation getProducts():Product[]
           operation getEsgData(EsgScoreId):EsgScore
         }
      """.trimIndent()
      )
      stub.addResponse(
         "getProducts",
         """[{ "productId" : 1, "factor" : 10 }, { "productId" : 2, "factor" : 20 }]"""
      )
      stub.addResponse("getEsgData", modifyDataSource = true) { _, args ->
         // echo back the input as the result, for asserting below
         listOf(vyne.tryParseJson("EsgScore", """{ "rating" : "${args.single().second.toRawObject()}" }"""))
      }
      val result = runBlocking {
         vyne.query(
            """find { Product[] }  as {
    productId: ProductId
    score: ESGRating
}[]"""
         ).rawObjects()
      }

      result.shouldContain(mapOf("productId" to 2, "score" to "AAA202"))
      result.shouldContain(mapOf("productId" to 1, "score" to "AAA101"))
   }
}
