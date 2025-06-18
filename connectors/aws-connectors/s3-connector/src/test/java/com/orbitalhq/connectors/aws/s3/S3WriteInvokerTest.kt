package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.firstTypedCollection
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.query.tracing.ObjectStoreRequest
import com.orbitalhq.query.tracing.ObjectStoreResponse
import com.orbitalhq.query.tracing.OperationSpanEventSource
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Ignore
import org.junit.Test

class S3WriteInvokerTest : BaseS3Test() {

   val baseSchema = """
         import com.orbitalhq.formats.Csv
         import com.orbitalhq.aws.s3.S3Service
         import com.orbitalhq.aws.s3.S3Operation
         import com.orbitalhq.aws.s3.FilenamePattern
         import com.orbitalhq.aws.s3.RequestBody
         import  ${VyneQlGrammar.QUERY_TYPE_NAME}
         type Price inherits Decimal
         type OpenPrice inherits Price
         type ClosePrice inherits Price
         type HighPrice inherits Price
         type Symbol inherits String

         closed model StockPrice {
            symbol : Symbol
            open : OpenPrice
            high : HighPrice
            close : ClosePrice
          }

         @com.orbitalhq.formats.Csv
         parameter type StockPriceCsv {
             ticker : Symbol
             openPrice : OpenPrice
             closePrice : ClosePrice
         }

   """.trimIndent()

   @Test
   fun `emits tracing events when writing to S3 and includes request payload`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      val schema = """
          $baseSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              write operation writeValue(@RequestBody payload: StockPriceCsv[], filename: FilenamePattern = "trades.csv"):StockPriceCsv[]
          }
          service ApiService {
            operation getPrices():StockPrice[]
         }
      """.trimIndent()
      val (vyne, stub) = vyneWithS3Invoker(schema)
      stub.addResponse(
         "getPrices",
         vyne.parseJson(
            "StockPrice[]",
            """[{ "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
            { "symbol" : "BTCUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }]
            """.trimMargin()
         )
      )

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val results = vyne.query(
         """
         |find { StockPrice[] } as StockPriceCsv[]
         |call AwsBucketService::writeValue
      """.trimMargin(),
         eventBroker = eventBroker
      ).firstTypedCollection()

      // Should have request and response events
      val s3Events = eventSink.collectedEvents
         .filter { it.exchangeMetadata is ObjectStoreRequest || it.exchangeMetadata is ObjectStoreResponse }
      s3Events.shouldHaveSize(2)

      // Verify request event
      val requestEvent = s3Events.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreRequest>()
      requestEvent.eventVerb.shouldBe("Write")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("trades.csv")

      // Verify response event
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreResponse>()
      responseEvent.eventVerb.shouldBe("Write response")
   }

   @Test
   fun `emits tracing events when writing single object to S3`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      val schema = """
          $baseSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              write operation writeValue(@RequestBody payload: StockPriceCsv, filename: FilenamePattern = "trade.csv"):StockPriceCsv
          }
          service ApiService {
            operation getPrices():StockPrice
         }
      """.trimIndent()
      val (vyne, stub) = vyneWithS3Invoker(schema)
      stub.addResponse(
         "getPrices",
         vyne.parseJson(
            "StockPrice",
            """{ "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }""".trimMargin()
         )
      )

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val results = vyne.query(
         """
         |given { FilenamePattern = "trade.csv" }
         |find { StockPrice } as StockPriceCsv
         |call AwsBucketService::writeValue
      """.trimMargin(),
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      val s3Events = eventSink.collectedEventsWithMetadataOfType(ObjectStoreRequest::class, ObjectStoreResponse::class)
      s3Events.shouldHaveSize(2)

      // Verify request event includes filename
      val requestEvent = s3Events.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreRequest>()
      requestEvent.eventVerb.shouldBe("Write")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("trade.csv")

      // Verify response event
      val responseEvent = s3Events.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreResponse>()
      responseEvent.eventVerb.shouldBe("Write response")

      // Verify actual results and file was written
      results.shouldHaveSize(1)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trade.csv") }
      val csv = String(response.readAllBytes())
      csv.trim().shouldBe("ticker,openPrice,closePrice\r\nGBPUSD,6262.37,6312.36")
   }

   @Test
   fun `can read collection from an operation returning a collection and write to a file`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      val schema = """
          $baseSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              write operation writeValue(@RequestBody payload: StockPriceCsv[], filename: FilenamePattern = "trades.csv"):StockPriceCsv[]
          }
          service ApiService {
            operation getPrices():StockPrice[]
         }
      """.trimIndent()
      val (vyne, stub) = vyneWithS3Invoker(schema)
      stub.addResponse(
         "getPrices",
         vyne.parseJson(
            "StockPrice[]",
            """[{ "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
            { "symbol" : "BTCUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
            { "symbol" : "GBPBTC", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }]
            """.trimMargin()
         )
      )

      val results = vyne.query(
         """
         |find { StockPrice[] } as StockPriceCsv[]
         |call AwsBucketService::writeValue
      """.trimMargin()
      )
         .firstTypedCollection()
      results.shouldHaveSize(3)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trades.csv") }
      val csv = String(response.readAllBytes()).trim().normalizeLineEndings()
      csv.shouldBe(
         """ticker,openPrice,closePrice
GBPUSD,6262.37,6312.36
BTCUSD,6262.37,6312.36
GBPBTC,6262.37,6312.36"""
      )
   }

   @Test
   fun `can write a single line to a csv file`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      val schema = """
          $baseSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              write operation writeValue(@RequestBody payload: StockPriceCsv, filename: FilenamePattern = "trade.csv"):StockPriceCsv
          }
          service ApiService {
            operation getPrices():StockPrice
         }
      """.trimIndent()
      val (vyne, stub) = vyneWithS3Invoker(schema)
      stub.addResponse(
         "getPrices",
         vyne.parseJson(
            "StockPrice",
            """{ "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }""".trimMargin()
         )
      )

      val results = vyne.query(
         """
         |given { FilenamePattern = "trade.csv" }
         |find { StockPrice } as StockPriceCsv
         |call AwsBucketService::writeValue
      """.trimMargin()
      )
         .rawObjects()
      results.shouldHaveSize(1)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trade.csv") }
      val csv = String(response.readAllBytes())
      csv.trim().shouldBe("ticker,openPrice,closePrice\r\nGBPUSD,6262.37,6312.36")
   }

   @Test
   fun `can read collection from within a response and write to a file`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      val schema = """
          $baseSchema

          model PriceSummary {
             prices : StockPrice[]
          }

          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              write operation writeValue(@RequestBody payload: StockPriceCsv[], filename: FilenamePattern = "trade.csv"):StockPriceCsv
          }
          service ApiService {
            operation getPrices():PriceSummary
         }
      """.trimIndent()
      val (vyne, stub) = vyneWithS3Invoker(schema)
      stub.addResponse(
         "getPrices",
         vyne.parseJson(
            "PriceSummary",
            """
            { "prices" : [
               { "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
               { "symbol" : "BTCUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
               { "symbol" : "GBPBTC", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }
            ]}
            """.trimMargin()
         )
      )

      val results = vyne.query(
         """
         |find { PriceSummary } as StockPrice[] as StockPriceCsv[]
         |call AwsBucketService::writeValue
      """.trimMargin()
      )
         .firstTypedCollection()
      results.shouldHaveSize(3)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trade.csv") }
      val csv = String(response.readAllBytes()).trim().normalizeLineEndings()
      csv.shouldBe(
         """ticker,openPrice,closePrice
GBPUSD,6262.37,6312.36
BTCUSD,6262.37,6312.36
GBPBTC,6262.37,6312.36"""
      )
   }
}

fun String.normalizeLineEndings(): String = this.replace("\r\n", "\n")
