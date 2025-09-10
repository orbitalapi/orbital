package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.Vyne
import com.orbitalhq.connectors.config.mongodb.MongoConnection
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.nosql.mongodb.registry.InMemoryMongoConnectionRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.testVyneWithStub
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MongoDeleteByQueryInvokerTest : MongoDbTestcontainer() {
   val baseSchema = """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}

         type ProductId inherits String
         type ProductName inherits String
         type CategoryId inherits String
         type Price inherits Decimal

         model BaseProduct {
            @Id
            id: ProductId
            name: ProductName
            categoryId: CategoryId
            price: Price
         }

         closed model ProductInfo inherits BaseProduct
         service ProductApi {
            operation getProductInfo():ProductInfo[]
         }

         @Collection(connection = "testMongo", collection = "products")
         closed parameter model Product inherits BaseProduct

         @MongoService(connection = "testMongo")
         service ProductDb {
             @UpsertOperation
             write operation insertProduct(Product):Product
             table products:Product[]
         }
   """.trimIndent()

   val productsDataset = """
[
  {
    "id": "laptop-1",
    "name": "Gaming Laptop",
    "categoryId": "electronics",
    "price": 1299.99
  },
  {
    "id": "phone-1",
    "name": "Smartphone",
    "categoryId": "electronics",
    "price": 899.99
  },
  {
    "id": "book-1",
    "name": "Programming Guide",
    "categoryId": "books",
    "price": 49.99
  },
  {
    "id": "tablet-1",
    "name": "Android Tablet",
    "categoryId": "electronics",
    "price": 399.99
  },
  {
    "id": "book-2",
    "name": "Design Patterns",
    "categoryId": "books",
    "price": 59.99
  }
]
   """.trimIndent()

   private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
   private lateinit var connectionFactory: MongoConnectionFactory

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   val TRIPLE_QUOTE = "\"\"\""

   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("testMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory = MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
   }


   @Test
   fun `can use MongoDelete for complex filtering`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteByQuery(
               collection = "products",
               filter = '{ categoryId: :categoryId, price: { ${'$'}gte: :minPrice } }'
            )
            write operation deleteExpensiveInCategory(
               categoryId: CategoryId,
               minPrice: Price
            ): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      // Delete electronics products >= $500
      val deleteResult = vyne.query("""
         given {
            categoryId: CategoryId = "electronics",
            minPrice: Price = 500.00
         }
         call ProductService::deleteExpensiveInCategory
      """.trimIndent()).rawObjects()

      // Should delete laptop (1299.99) and phone (899.99), but not tablet (399.99)
      (deleteResult.first()["deletedCount"] as Int).shouldBe(2)

      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(3)

      val remainingIds = remainingProducts.map { it["id"] as String }
      remainingIds shouldContainAll listOf("tablet-1", "book-1", "book-2")
   }

   @Test
   fun `MongoDelete can delete all documents with empty filter`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteByQuery(
               collection = "products",
               filter = '{}'
            )
            write operation deleteAllProducts(): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      val deleteResult = vyne.query("""
         call ProductService::deleteAllProducts
      """.trimIndent()).rawObjects()

      (deleteResult.first()["deletedCount"] as Int).shouldBe(5)

      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(0)
   }

   @Test
   fun `MongoDelete with multiple conditions and operators`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteByQuery(
               collection = "products",
               filter = $TRIPLE_QUOTE{
                  ${'$'}or: [
                     { categoryId: :category1 },
                     {
                        price: { ${'$'}lt: :maxPrice }
                     }
                  ]
               }$TRIPLE_QUOTE
            )
            write operation deleteByComplexCriteria(
               category1: CategoryId,
               maxPrice: Price
            ): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      // Delete: all books OR electronics under $500
      val deleteResult = vyne.query("""
         given {
            category1: CategoryId = "books",
            maxPrice: Price = 500.00
         }
         call ProductService::deleteByComplexCriteria
      """.trimIndent()).rawObjects()

      // Should delete: book-1 (49.99), book-2 (59.99), tablet-1 (399.99)
      // Should keep: laptop-1 (1299.99), phone-1 (899.99)
      (deleteResult.first()["deletedCount"] as Int).shouldBe(3)

      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(2)

      val remainingIds = remainingProducts.map { it["id"] as String }
      remainingIds shouldContainAll listOf("laptop-1", "phone-1")
   }

   private fun insertSampleData(vyne: Vyne, stub: StubService) = runBlocking {
      stub.addResponse("getProductInfo", productsDataset)
      val insertResult = vyne.query("""
         find { ProductInfo[] }
         call ProductDb::insertProduct
      """.trimIndent()).rawObjects()
      logger.info { "Inserted ${insertResult.size} records" }
   }
}
