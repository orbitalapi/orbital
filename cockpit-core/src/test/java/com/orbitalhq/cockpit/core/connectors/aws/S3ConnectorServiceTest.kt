package com.orbitalhq.cockpit.core.connectors.aws

import com.google.common.io.Resources
import com.orbitalhq.connectors.aws.s3.BaseS3Test
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import kotlin.io.path.toPath
import kotlin.io.path.writeText

class S3ConnectorServiceTest : BaseS3Test() {

   @Rule
   @JvmField
   val tempFolder = TemporaryFolder()

   @Test
   fun `can list files in a bucket that match a prefix`() {
      val bucketName = createBucketWithRandomName("Trades")
      uploadResourceToS3(bucketName, "trades1.csv", tempFile("trades1.csv"))
      uploadResourceToS3(bucketName, "trades2.csv", tempFile("trades2.csv"))
      uploadResourceToS3(bucketName, "films.csv", tempFile("films.csv"))

      val service = S3ConnectorService(connectionRegistry)

      service.listFilenames(AWS_CONNECTION_NAME, bucketName, "*.csv")
         .block()!!
         .objectNames
         .shouldContainExactlyInAnyOrder("trades1.csv", "trades2.csv", "films.csv")

      service.listFilenames(AWS_CONNECTION_NAME, bucketName, "trade*.csv")
         .block()!!
         .objectNames
         .shouldContainExactlyInAnyOrder("trades1.csv", "trades2.csv")

      service.listFilenames(AWS_CONNECTION_NAME, bucketName, "*")
         .block()!!
         .objectNames
         .shouldContainExactlyInAnyOrder("trades1.csv", "trades2.csv", "films.csv")

   }

   @Test
   fun `can list s3 buckets`() {
      val bucketName = createBucketWithRandomName("Trades")
      val service = S3ConnectorService(connectionRegistry)

      val response = service.listBuckets(AWS_CONNECTION_NAME)
         .block()
      response.bucketNames.shouldHaveSize(1)
      response.bucketNames.single().shouldBe(bucketName)
   }


   private fun tempFile(name: String): Path {
      val path = tempFolder.root.toPath().resolve(name)
      path.writeText("test")
      return path
   }
}
