package com.orbitalhq.connectors.aws.s3

import com.google.common.io.Resources
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Test
import kotlin.io.path.toPath


class S3ReadInvokerTest : BaseS3Test() {

   val baseSchema = """
         import com.orbitalhq.formats.Csv
         import com.orbitalhq.aws.s3.S3Service
         import com.orbitalhq.aws.s3.S3Operation
         import  ${VyneQlGrammar.QUERY_TYPE_NAME}
         type Price inherits Decimal
         type OpenPrice inherits Price
         type ClosePrice inherits Price
         type HighPrice inherits Price
         type Symbol inherits String
   """.trimIndent()
   val columnIndexedSchema = """
         @com.orbitalhq.formats.Csv
         closed model OrderSummary {
             symbol : Symbol by column(2)
             open : OpenPrice by column(3)
             high : HighPrice by column(4)
             close : ClosePrice by column(6)
         }
   """.trimIndent()
   val namedFieldsSchema = """
         @com.orbitalhq.formats.Csv
         closed model OrderSummary {
             Symbol : Symbol
             Open : OpenPrice
             High : HighPrice
             Close : ClosePrice
         }
   """.trimIndent()
   @Test
   fun `can read a single file from s3 using a default value for filename and column indexed csv`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      uploadResourceToS3(bucketName, "trades.csv", Resources.getResource("Coinbase_BTCUSD_3rows.csv").toURI().toPath())
      val schema = """
          $baseSchema
          $columnIndexedSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              operation readBucket(filename:FilenamePattern = "trades.csv"):OrderSummary[]
          }
      """.trimIndent()
      val (vyne) = vyneWithS3Invoker(schema)

      val results = vyne.query("""find { OrderSummary[] }""")
         .rawObjects()

      results.shouldHaveSize(3)
      results.shouldContainExactlyInAnyOrder(
         mapOf(
            "symbol" to "BTCUSD",
            "open" to 6300.toBigDecimal(),
            "high" to 6330.toBigDecimal(),
            "close" to 6235.2.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCUSD",
            "open" to 6262.37.toBigDecimal(),
            "high" to 6441.37.toBigDecimal(),
            "close" to 6300.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCUSD",
            "open" to 6257.15.toBigDecimal(),
            "high" to 6282.12.toBigDecimal(),
            "close" to 6262.37.toBigDecimal(),
         ),
      )
   }

   @Test
   fun `can read a single file from s3 using a default value for filename and name indexed csv`(): Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      uploadResourceToS3(bucketName, "trades.csv", Resources.getResource("Coinbase_BTCUSD_3rows.csv").toURI().toPath())
      val schema = """
          $baseSchema
          $namedFieldsSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              operation readBucket(filename:FilenamePattern = "trades.csv"):OrderSummary[]
          }
      """.trimIndent()
      val (vyne) = vyneWithS3Invoker(schema)

      val results = vyne.query("""find { OrderSummary[] }""")
         .rawObjects()

      results.shouldHaveSize(3)
      results.shouldContainExactlyInAnyOrder(
         mapOf(
            "Symbol" to "BTCUSD",
            "Open" to 6300.toBigDecimal(),
            "High" to 6330.toBigDecimal(),
            "Close" to 6235.2.toBigDecimal(),
         ),
         mapOf(
            "Symbol" to "BTCUSD",
            "Open" to 6262.37.toBigDecimal(),
            "High" to 6441.37.toBigDecimal(),
            "Close" to 6300.toBigDecimal(),
         ),
         mapOf(
            "Symbol" to "BTCUSD",
            "Open" to 6257.15.toBigDecimal(),
            "High" to 6282.12.toBigDecimal(),
            "Close" to 6262.37.toBigDecimal(),
         ),
      )
   }

   @Test
   fun `can read multiple files from s3 using a default value for filename`():Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      uploadResourceToS3(bucketName, "trades1.csv", Resources.getResource("Coinbase_BTCUSD_3rows.csv").toURI().toPath())
      uploadResourceToS3(bucketName, "trades2.csv", Resources.getResource("Coinbase_BTCGBP_3rows.csv").toURI().toPath())
      uploadResourceToS3(bucketName, "orders1.csv", Resources.getResource("Coinbase_BTCGBP_3rows.csv").toURI().toPath())
      val schema = """
         $baseSchema
         $columnIndexedSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              operation readBucket(filename:FilenamePattern = "trades*.csv"):OrderSummary[]
          }
      """.trimIndent()
      val (vyne) = vyneWithS3Invoker(schema)
      val results = vyne.query("""find { OrderSummary[] }""")
         .rawObjects()

      results.shouldHaveSize(6)
      results.shouldContainExactlyInAnyOrder(
         mapOf(
            "symbol" to "BTCUSD",
            "open" to 6300.toBigDecimal(),
            "high" to 6330.toBigDecimal(),
            "close" to 6235.2.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCUSD",
            "open" to 6262.37.toBigDecimal(),
            "high" to 6441.37.toBigDecimal(),
            "close" to 6300.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCUSD",
            "open" to 6257.15.toBigDecimal(),
            "high" to 6282.12.toBigDecimal(),
            "close" to 6262.37.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCGBP",
            "open" to 6300.toBigDecimal(),
            "high" to 6330.toBigDecimal(),
            "close" to 6235.2.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCGBP",
            "open" to 6262.37.toBigDecimal(),
            "high" to 6441.37.toBigDecimal(),
            "close" to 6300.toBigDecimal(),
         ),
         mapOf(
            "symbol" to "BTCGBP",
            "open" to 6257.15.toBigDecimal(),
            "high" to 6282.12.toBigDecimal(),
            "close" to 6262.37.toBigDecimal(),
         ),
      )

   }

   @Test
   fun `can read a file from s3 using a argument for filename`():Unit = runBlocking {
      val bucketName = createBucketWithRandomName("Trades")
      uploadResourceToS3(bucketName, "trades1.csv", Resources.getResource("Coinbase_BTCUSD_3rows.csv").toURI().toPath())
      uploadResourceToS3(bucketName, "trades2.csv", Resources.getResource("Coinbase_BTCGBP_3rows.csv").toURI().toPath())
      uploadResourceToS3(bucketName, "orders1.csv", Resources.getResource("Coinbase_BTCGBP_3rows.csv").toURI().toPath())
      val schema = """
          $baseSchema
          $columnIndexedSchema
          @S3Service( connectionName = "$AWS_CONNECTION_NAME" )
          service AwsBucketService {
              @S3Operation(bucket = "$bucketName")
              operation readBucket(filename:FilenamePattern):OrderSummary[]
          }
      """.trimIndent()
      val (vyne) = vyneWithS3Invoker(schema)

      val results = vyne.query("""
         given { pattern: FilenamePattern = "trade*.csv" }
         find { OrderSummary[] }""".trimIndent())
         .rawObjects()

      // We want the 6 trades, but not the 3 from orders1.csv
      results.shouldHaveSize(6)
   }
}
