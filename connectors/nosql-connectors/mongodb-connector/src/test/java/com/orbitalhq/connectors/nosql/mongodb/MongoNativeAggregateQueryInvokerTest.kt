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
import com.orbitalhq.typedInstances
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MongoNativeAggregateQueryInvokerTest : MongoDbTestcontainer() {

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
            // Mock HTTP service to return the data we'll load into Mongo
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

   @Test
   fun `can execute basic aggregation pipeline with match stage`(): Unit = runBlocking {
      val schema = """
         $baseSchema

         model ProductByCategory inherits Product
         @MongoService(connection = "testMongo")
         service ProductService {
            @CollectionAggregation(
               pipeline = {
                  collection: "products",
                  stages: [
                     '{ ${'$'}match: { categoryId: :categoryId } }',
                     '{ ${'$'}sort: { price: -1 } }'
                  ]
               }

            )
            operation getProductsByCategory(categoryId: CategoryId): ProductByCategory[](...)
         }
      """.trimIndent()

      val (vyne,stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne,stub)

      val results = vyne.query("""find { ProductByCategory[](CategoryId == "electronics") }""")
         .rawObjects()

      results.shouldHaveSize(3)
      // Should be sorted by price descending
      results[0].shouldBe(
         mapOf(
            "id" to "laptop-1",
            "name" to "Gaming Laptop",
            "categoryId" to "electronics",
            "price" to 1299.99.toBigDecimal()
         )
      )
   }

   private fun insertSampleData(vyne: Vyne, stub:StubService) = runBlocking {
      stub.addResponse("getProductInfo", productsDataset)
      val insertResult = vyne.query("""
         find { ProductInfo[] }
         call ProductDb::insertProduct
      """.trimIndent()).rawObjects()
      logger.info { "Inserted ${insertResult.size} records" }
   }

   @Test
   fun `can execute aggregation with group stage for counting`(): Unit = runBlocking {
      val schema = """
      $baseSchema

      type CategoryName inherits String
      type ProductCount inherits Int

      closed model CategorySummary {
         categoryId: CategoryId
         categoryName: CategoryName
         productCount: ProductCount
      }

      @MongoService(connection = "testMongo")
      service AnalyticsService {
         @CollectionAggregation(
            pipeline = {
               collection: "products",
               stages: [
                  '{ ${'$'}addFields: { categoryName:  { ${'$'}switch: { branches: [ { case: { ${'$'}eq: ["${'$'}categoryId", "electronics"] }, then: "Electronics" }, { case: { ${'$'}eq: ["${'$'}categoryId", "books"] }, then: "Books" } ], default: "Other" } } } }',
                  '{ ${'$'}group: { _id: "${'$'}categoryId", categoryName: { ${'$'}first: "${'$'}categoryName" }, count: { ${'$'}sum: 1 } } }',
                  '{ ${'$'}project: { categoryId: "${'$'}_id", categoryName: 1, productCount: "${'$'}count", _id: 0 } }'
               ]
            }

         )
         operation getCategorySummary(): CategorySummary[]
      }
   """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      val results = vyne.query("""find { CategorySummary[] }""")
         .rawObjects()

      results.shouldHaveSize(2)
      results.forEach { result ->
         val categoryId = result["categoryId"] as String
         when (categoryId) {
            "electronics" -> {
               result["categoryName"].shouldBe("Electronics")
               result["productCount"].shouldBe(3)  // laptop-1, phone-1, tablet-1
            }
            "books" -> {
               result["categoryName"].shouldBe("Books")
               result["productCount"].shouldBe(2)  // book-1, book-2
            }
         }
      }
   }


   @Test
   fun `can execute aggregation with pagination using facet`(): Unit = runBlocking {
      val schema = """
      $baseSchema

      type TotalRecords inherits Int
      type Page inherits Int
      type TotalPages inherits Int

      type Offset inherits Int
      type PageSize inherits Int

      model PaginationMeta {
         totalRecords: TotalRecords
         page: Page
         totalPages: TotalPages
      }

      model ProductPage {
         results: Product[]
         meta: PaginationMeta
      }

      @MongoService(connection = "testMongo")
      service ProductService {
         @CollectionAggregation(
            pipeline = {
               collection: "products",
               stages: [
                  '{ ${'$'}sort: { price: -1 } }',
                  $TRIPLE_QUOTE{ ${'$'}facet: {
                     "results": [
                        { ${'$'}skip: :offset },
                        { ${'$'}limit: :pageSize }
                     ],
                     "metadata": [
                        { ${'$'}count: "totalRecords" }
                     ]
                  } }$TRIPLE_QUOTE,
                  $TRIPLE_QUOTE{ ${'$'}addFields: {
                     "meta": {
                        "totalRecords": { ${'$'}arrayElemAt: ["${'$'}metadata.totalRecords", 0] },
                        "page": { ${'$'}add: [{ ${'$'}divide: [:offset, :pageSize] }, 1] },
                        "totalPages": { ${'$'}ceil: { ${'$'}divide: [{ ${'$'}arrayElemAt: ["${'$'}metadata.totalRecords", 0] }, :pageSize] } }
                     }
                  } }$TRIPLE_QUOTE,
                  $TRIPLE_QUOTE{ ${'$'}project: {
                     "results": 1,
                     "meta": 1
                  } }$TRIPLE_QUOTE
               ]
            }
         )
         operation getProductsWithPagination(
            offset: Offset,
            pageSize: PageSize
         ): ProductPage(...)
      }
   """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      // Test first page (offset 0, pageSize 2)
      val firstPageResults = vyne.query("""
      given {
         offset: Offset = 0,
         pageSize: PageSize = 2
      }
      find { ProductPage(Offset == offset && PageSize == pageSize) }
   """.trimIndent())
         .rawObjects()

      firstPageResults.shouldHaveSize(1)
      val firstPage = firstPageResults.first() as Map<String, Any>

      // Check results - should be top 2 products by price
      val firstPageProducts = firstPage["results"] as List<Map<String, Any>>
      firstPageProducts.shouldHaveSize(2)
      firstPageProducts[0]["price"].shouldBe(1299.99.toBigDecimal()) // Gaming Laptop
      firstPageProducts[1]["price"].shouldBe(899.99.toBigDecimal())  // Smartphone

      // Check metadata
      val firstPageMeta = firstPage["meta"] as Map<String, Any>
      firstPageMeta["totalRecords"].shouldBe(5)
      firstPageMeta["page"].shouldBe(1)
      firstPageMeta["totalPages"].shouldBe(3) // 5 records / 2 per page = 3 pages

      // Test second page (offset 2, pageSize 2)
      val secondPageResults = vyne.query("""
      given {
         offset: Offset = 2,
         pageSize: PageSize = 2
      }
      find { ProductPage(Offset == offset && PageSize == pageSize) }
   """.trimIndent())
         .rawObjects()

      secondPageResults.shouldHaveSize(1)
      val secondPage = secondPageResults.first() as Map<String, Any>

      // Check results - should be next 2 products by price
      val secondPageProducts = secondPage["results"] as List<Map<String, Any>>
      secondPageProducts.shouldHaveSize(2)
      secondPageProducts[0]["price"].shouldBe(399.99.toBigDecimal()) // Android Tablet
      secondPageProducts[1]["price"].shouldBe(59.99.toBigDecimal())  // Design Patterns
      firstPage
      // Check metadata
      val secondPageMeta = secondPage["meta"] as Map<String, Any>
      secondPageMeta["totalRecords"].shouldBe(5)
      secondPageMeta["page"].shouldBe(2)
      secondPageMeta["totalPages"].shouldBe(3)

      // Test last page (offset 4, pageSize 2)
      val lastPageResults = vyne.query("""
      given {
         offset: Offset = 4,
         pageSize: PageSize = 2
      }
      find { ProductPage(Offset == offset && PageSize == pageSize) }
   """.trimIndent())
         .rawObjects()

      lastPageResults.shouldHaveSize(1)
      val lastPage = lastPageResults.first() as Map<String, Any>

      // Check results - should be only 1 product left
      val lastPageProducts = lastPage["results"] as List<Map<String, Any>>
      lastPageProducts.shouldHaveSize(1)
      lastPageProducts[0]["price"].shouldBe(49.99.toBigDecimal()) // Programming Guide

      // Check metadata
      val lastPageMeta = lastPage["meta"] as Map<String, Any>
      lastPageMeta["totalRecords"].shouldBe(5)
      lastPageMeta["page"].shouldBe(3)
      lastPageMeta["totalPages"].shouldBe(3)
   }

   @Test
   fun `aggregation with empty result set returns empty list`(): Unit = runBlocking {
      val schema = """
      $baseSchema

      model ProductByNonexistentCategory inherits Product

      @MongoService(connection = "testMongo")
      service ProductService {
         @CollectionAggregation(
            pipeline = {
               collection: "products",
               stages: [
                  '{ ${'$'}match: { categoryId: :categoryId } }'
               ]
            }

         )
         operation getProductsByCategory(categoryId: CategoryId): ProductByNonexistentCategory[](...)
      }
   """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      insertSampleData(vyne, stub)

      val results = vyne.query("""find { ProductByNonexistentCategory[](CategoryId == "nonexistent") }""")
         .rawObjects()

      results.shouldHaveSize(0)
   }

   @Test
   fun `can execute aggregation with lookup stage for joining collections`(): Unit = runBlocking {
      val schema = """
      $baseSchema

      type OrderId inherits String
      type CustomerId inherits String
      type CustomerName inherits String
      type OrderTotal inherits Decimal

      @Collection(connection = "testMongo", collection = "customers")
      closed parameter model Customer {
         @Id
         id: CustomerId
         name: CustomerName
      }

      @Collection(connection = "testMongo", collection = "orders")
      closed parameter model Order {
         @Id
         id: OrderId
         customerId: CustomerId
         total: OrderTotal
      }

      model OrderWithCustomer {
         orderId: OrderId
         customerName: CustomerName
         total: OrderTotal
      }

      @MongoService(connection = "testMongo")
      service OrderDb {
         @UpsertOperation
         write operation insertCustomer(Customer): Customer

         @UpsertOperation
         write operation insertOrder(Order): Order
      }

      @MongoService(connection = "testMongo")
      service OrderService {
         @CollectionAggregation(
            pipeline = {
               collection: "orders",
               stages: [
                  $TRIPLE_QUOTE{ ${'$'}lookup: {
                     from: "customers",
                     localField: "customerId",
                     foreignField: "_id",
                     as: "customer"
                  } }$TRIPLE_QUOTE,
                  '{ ${'$'}unwind: "${'$'}customer" }',
                  $TRIPLE_QUOTE{ ${'$'}project: {
                     orderId: "${'$'}_id",
                     customerName: "${'$'}customer.name",
                     total: 1
                  } }$TRIPLE_QUOTE
               ]
            }
         )
         operation getOrdersWithCustomerDetails(): OrderWithCustomer[]
      }
   """.trimIndent()

      val (vyne, stub) = testVyneWithStub(listOf(MongoConnector.schema, VyneQlGrammar.QUERY_TYPE_TAXI, schema)) { schema ->
         listOf(MongoDbInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry()))
      }

      // Insert test customers
      vyne.query("""
      given { Customer = { id: "cust-1", name: "Alice Johnson" } }
      call OrderDb::insertCustomer
   """.trimIndent()).typedInstances()

      vyne.query("""
      given { Customer = { id: "cust-2", name: "Bob Smith" } }
      call OrderDb::insertCustomer
   """.trimIndent()).typedInstances()

      vyne.query("""
      given { Customer = { id: "cust-3", name: "Charlie Brown" } }
      call OrderDb::insertCustomer
   """.trimIndent()).typedInstances()

      // Insert test orders
      vyne.query("""
      given { Order = { id: "order-1", customerId: "cust-1", total: 250.00 } }
      call OrderDb::insertOrder
   """.trimIndent()).typedInstances()

      vyne.query("""
      given { Order = { id: "order-2", customerId: "cust-2", total: 175.50 } }
      call OrderDb::insertOrder
   """.trimIndent()).typedInstances()

      vyne.query("""
      given { Order = { id: "order-3", customerId: "cust-1", total: 89.99 } }
      call OrderDb::insertOrder
   """.trimIndent()).typedInstances()

      // Execute the lookup aggregation
      val results = vyne.query("""find { OrderWithCustomer[] }""")
         .rawObjects()

      results.shouldHaveSize(3)

      // Verify the joined data
      results.forEach { result ->
         result.keys.shouldContain("orderId")
         result.keys.shouldContain("customerName")
         result.keys.shouldContain("total")
         result["customerName"].shouldNotBeNull()
      }

      // Check specific order-customer combinations
      val order1 = results.find { it["orderId"] == "order-1" }!!
      order1["customerName"].shouldBe("Alice Johnson")
      order1["total"].shouldBe("250.00".toBigDecimal())

      val order2 = results.find { it["orderId"] == "order-2" }!!
      order2["customerName"].shouldBe("Bob Smith")
      order2["total"].shouldBe("175.50".toBigDecimal())

      val order3 = results.find { it["orderId"] == "order-3" }!!
      order3["customerName"].shouldBe("Alice Johnson") // Same customer as order-1
      order3["total"].shouldBe("89.99".toBigDecimal())

      // Verify we have the expected customer names
      val customerNames = results.map { it["customerName"] as String }.toSet()
      customerNames shouldContain "Alice Johnson"
      customerNames shouldContain "Bob Smith"
      customerNames.shouldHaveSize(2) // Only 2 unique customers in our orders
   }
}
