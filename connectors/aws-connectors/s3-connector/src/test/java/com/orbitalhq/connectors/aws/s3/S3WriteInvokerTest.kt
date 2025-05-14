package com.orbitalhq.connectors.aws.s3

import com.orbitalhq.firstTypedCollection
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.Ignore
import org.junit.Test

class S3WriteInvokerTest  : BaseS3Test() {

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
   fun `can read collection from an operation returning a collection and write to a file`():Unit = runBlocking {
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
      val (vyne,stub) = vyneWithS3Invoker(schema)
      stub.addResponse("getPrices",
         vyne.parseJson("StockPrice[]",
            """[{ "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
            { "symbol" : "BTCUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
            { "symbol" : "GBPBTC", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }]
            """.trimMargin()))

      val results = vyne.query("""
         |find { StockPrice[] } as StockPriceCsv[]
         |call AwsBucketService::writeValue
      """.trimMargin())
         .firstTypedCollection()
      results.shouldHaveSize(3)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trades.csv") }
      val csv = String(response.readAllBytes()).trim().normalizeLineEndings()
      csv.shouldBe("""ticker,openPrice,closePrice
GBPUSD,6262.37,6312.36
BTCUSD,6262.37,6312.36
GBPBTC,6262.37,6312.36""")
   }

   @Test
   fun `can write a single line to a csv file`():Unit = runBlocking {
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
      val (vyne,stub) = vyneWithS3Invoker(schema)
      stub.addResponse("getPrices",
         vyne.parseJson("StockPrice",
            """{ "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }""".trimMargin()))

      val results = vyne.query("""
         |given { FilenamePattern = "trade.csv" }
         |find { StockPrice } as StockPriceCsv
         |call AwsBucketService::writeValue
      """.trimMargin())
         .rawObjects()
      results.shouldHaveSize(1)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trade.csv") }
      val csv = String(response.readAllBytes())
      csv.trim().shouldBe("ticker,openPrice,closePrice\r\nGBPUSD,6262.37,6312.36")
   }

   @Test
   fun `can read collection from within a response and write to a file`():Unit = runBlocking {
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
      val (vyne,stub) = vyneWithS3Invoker(schema)
      stub.addResponse("getPrices",
         vyne.parseJson("PriceSummary",
            """
            { "prices" : [
               { "symbol" : "GBPUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
               { "symbol" : "BTCUSD", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 },
               { "symbol" : "GBPBTC", "open" : 6262.37, "high" : 6441.37, "close" : 6312.36 }
            ]}
            """.trimMargin()))

      val results = vyne.query("""
         |find { PriceSummary } as StockPrice[] as StockPriceCsv[]
         |call AwsBucketService::writeValue
      """.trimMargin())
         .firstTypedCollection()
      results.shouldHaveSize(3)
      val response = s3Client.getObject { builder -> builder.bucket(bucketName).key("trade.csv") }
      val csv = String(response.readAllBytes()).trim().normalizeLineEndings()
      csv.shouldBe("""ticker,openPrice,closePrice
GBPUSD,6262.37,6312.36
BTCUSD,6262.37,6312.36
GBPBTC,6262.37,6312.36""")
   }
}

fun String.normalizeLineEndings():String = this.replace("\r\n", "\n")
