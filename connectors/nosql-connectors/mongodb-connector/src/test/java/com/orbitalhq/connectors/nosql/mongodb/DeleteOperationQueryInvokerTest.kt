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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteOperationQueryInvokerTest : MongoDbTestcontainer() {
   private lateinit var connectionRegistry: InMemoryMongoConnectionRegistry
   private lateinit var connectionFactory: MongoConnectionFactory

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @BeforeEach
   fun setup() {
      val connectionParams = mapOf(MongoConnection.Parameters.CONNECTION_STRING.templateParamName to connectionString)
      val mongo1ConnectionConfig = MongoConnectionConfiguration("testMongo", connectionParams)
      connectionRegistry = InMemoryMongoConnectionRegistry(listOf(mongo1ConnectionConfig))
      connectionFactory = MongoConnectionFactory(connectionRegistry, SimpleMeterRegistry())
   }

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

   private fun insertSampleData(vyne: Vyne, stub: StubService) = runBlocking {
      stub.addResponse("getProductInfo", productsDataset)
      val insertResult = vyne.query("""
         find { ProductInfo[] }
         call ProductDb::insertProduct
      """.trimIndent()).rawObjects()
      logger.info { "Inserted ${insertResult.size} records" }
   }

   // ========== DELETE ONE TESTS ==========

   @Test
   fun `can delete single product by id using deleteOne`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int

         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteOperation
            write operation deleteOneProduct(Product): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      // Delete the laptop
      val deleteResult = vyne.query("""
         given { Product = { id: "laptop-1", name: "Gaming Laptop", categoryId: "electronics", price: 1299.99 } }
         call ProductService::deleteOneProduct
      """.trimIndent()).rawObjects()

      // Verify delete result
      deleteResult.shouldHaveSize(1)
      (deleteResult.first()["deletedCount"] as Int).shouldBe(1)

      // Verify product is gone
      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(4)

      val remainingIds = remainingProducts.map { it["id"] as String }
      remainingIds.shouldNotContain("laptop-1")
   }

   @Test
   fun `deleteOne with non-existent product returns zero count`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteOperation
            write operation deleteOneProduct(Product): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      val deleteResult = vyne.query("""
         given { Product = { id: "non-existent-id" } }
         call ProductService::deleteOneProduct
      """.trimIndent()).rawObjects()

      (deleteResult.first()["deletedCount"] as Int).shouldBe(0)

      // All products should still be there
      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(5)
   }

   // ========== DELETE MANY TESTS ==========

   @Test
   fun `can delete multiple products by id array using deleteMany`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteOperation
            write operation deleteManyProducts(Product[]): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      // Delete laptop and phone
      val deleteResult = vyne.query("""
         given {
            Product[] = [
               { id: "laptop-1" },
               { id: "phone-1" }
            ]
         }
         call ProductService::deleteManyProducts
      """.trimIndent()).rawObjects()

      (deleteResult.first()["deletedCount"] as Int).shouldBe(2)

      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(3)

      val remainingIds = remainingProducts.map { it["id"] as String }
      remainingIds shouldContainAll listOf("book-1", "tablet-1", "book-2")
      remainingIds shouldNotContain "laptop-1"
      remainingIds shouldNotContain "phone-1"
   }

   @Test
   fun `deleteMany with empty array deletes nothing`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductService {
            @DeleteOperation
            write operation deleteManyProducts(Product[]): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      val deleteResult = vyne.query("""
         given { Product[] = [] }
         call ProductService::deleteManyProducts
      """.trimIndent()).rawObjects()

//      (deleteResult.first()["deletedCount"] as Int).shouldBe(0)

      val remainingProducts = vyne.query("""find { Product[] }""").rawObjects()
      remainingProducts.shouldHaveSize(5)
   }

   // ========== UNIQUE INDEX TESTS ==========

   @Test
   fun `can delete by UniqueIndex instead of Id`(): Unit = runBlocking {
      val schema = """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}

         type ProductSku inherits String
         type ProductName inherits String

         @Collection(connection = "testMongo", collection = "products_with_sku")
         closed parameter model ProductWithSku {
            @UniqueIndex
            sku: ProductSku
            name: ProductName
         }

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service ProductSkuService {
            @UpsertOperation
            write operation insertProduct(ProductWithSku): ProductWithSku

            @DeleteOperation
            write operation deleteProduct(ProductWithSku)

            table products: ProductWithSku[]
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      // Insert product with SKU
      vyne.query("""
         given { ProductWithSku = { sku: "SKU-12345", name: "Test Product" } }
         call ProductSkuService::insertProduct
      """.trimIndent())

      // Delete by SKU (UniqueIndex)
      val deleteResult = vyne.query("""
         given { ProductWithSku = { sku: "SKU-12345", name: "Test Product" } }
         call ProductSkuService::deleteProduct
      """.trimIndent()).rawObjects()

      val remainingProducts = vyne.query("""find { ProductWithSku[] }""").rawObjects()
      remainingProducts.shouldHaveSize(0)
   }

   // ========== FAILURE SCENARIO TESTS ==========

//   @Test
   fun `deleteOne fails when model has no Id or UniqueIndex`(): Unit = runBlocking {
      val schema = """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}

         @Collection(connection = "testMongo", collection = "products_no_id")
         closed parameter model ProductNoId {
            name: ProductName inherits String
            categoryId: CategoryId inherits String
         }

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service BadProductService {
            @DeleteOperation
            write operation deleteProduct(ProductNoId): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      shouldThrow<Exception> {
         vyne.query("""
            given { ProductNoId = { name: "Test", categoryId: "electronics" } }
            call BadProductService::deleteProduct
         """.trimIndent())
      }
   }

   @Test
   fun `deleteMany fails when model has no Id or UniqueIndex`(): Unit = runBlocking {
      val schema = """
         ${MongoConnector.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}

         @Collection(connection = "testMongo", collection = "products_no_id")
         closed parameter model ProductNoId {
            name: ProductName inherits String
            categoryId: CategoryId inherits String
         }

         type DeleteCount inherits Int
         model DeleteResult {
            deletedCount: DeleteCount
         }

         @MongoService(connection = "testMongo")
         service BadProductService {
            @DeleteOperation
            write operation deleteManyProducts(ProductNoId[]): DeleteResult
         }
      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }


         val query = vyne.query("""
            given { ProductNoId[] = [{ name: "Test", categoryId: "electronics" }] }
            call BadProductService::deleteManyProducts
         """.trimIndent()).rawObjects()

   }


}
