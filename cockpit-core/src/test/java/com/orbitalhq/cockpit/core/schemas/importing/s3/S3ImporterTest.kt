package com.orbitalhq.cockpit.core.schemas.importing.s3

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConversionRequest
import com.orbitalhq.cockpit.core.schemas.importing.concatenatedSource
import com.orbitalhq.connectors.aws.s3.BaseS3Test
import com.orbitalhq.connectors.aws.s3.S3ConnectorTaxi
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.NotFoundException
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import kotlin.io.path.writeText
import com.orbitalhq.shouldCompileTheSameAs
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.testing.TestHelpers

class S3ImporterTest : BaseS3Test() {

   @Rule
   @JvmField
   val tempFolder = TemporaryFolder()

   //To test: File not found
   // Unsupported file extension

   @Test
   fun `throws meaningful error if file not found`() {
      val bucketName = createBucketWithRandomName("Person")
      val importerService = S3Importer(
         SimpleSchemaProvider(TaxiSchema.empty()),
         connectionRegistry
      )

      val exception = assertThrows<NotFoundException> {
         importerService.convert(
            SchemaConversionRequest(
               "s3", emptyMap<String, Any>(), PackageIdentifier.fromId("com.foo/test/0.1.0")
            ),
            S3ConverterOptions(
               AWS_CONNECTION_NAME,
               bucketName,
               "com.foo.PersonData".fqn(),
               filenamePattern = "*.csv",
               fileToGenerateSchemaFrom = "IAmNotAValidFile.csv",
            )
         ).block()!!
      }
      exception.message.shouldBe("File IAmNotAValidFile.csv was not found in bucket $bucketName")
   }

   @Test
   fun `throws meaningful error if bucket not found`() {
      val importerService = S3Importer(
         SimpleSchemaProvider(TaxiSchema.empty()),
         connectionRegistry
      )

      val exception = assertThrows<NotFoundException> {
         importerService.convert(
            SchemaConversionRequest(
               "s3", emptyMap<String, Any>(), PackageIdentifier.fromId("com.foo/test/0.1.0")
            ),
            S3ConverterOptions(
               connectionName = AWS_CONNECTION_NAME,
               bucketName = "iAmNotAValidBucket",
               fileSchemaType = "com.foo.PersonData".fqn(),
               filenamePattern = "*.csv",
               fileToGenerateSchemaFrom = "person1.csv",
            )
         ).block()!!
      }
      exception.message.shouldBe("Bucket iAmNotAValidBucket does not exist")
   }

   @Test
   fun `can add an s3 connection and generate schema`() {
      val bucketName = createBucketWithRandomName("Person")
      uploadResourceToS3(bucketName, "person1.csv", tempFile("person1.csv"))

      val importerService = S3Importer(
         SimpleSchemaProvider(TaxiSchema.fromStrings(S3ConnectorTaxi.schema)),
         connectionRegistry
      )

      val result = importerService.convert(
         SchemaConversionRequest(
            "s3", emptyMap<String, Any>(), PackageIdentifier.fromId("com.foo/test/0.1.0")
         ),
         S3ConverterOptions(
            AWS_CONNECTION_NAME,
            bucketName,
            "com.foo.PersonData".fqn(),
            filenamePattern = "*.csv",
            fileToGenerateSchemaFrom = "person1.csv",
         )
      ).block()!!
      val generatedSrcs = result.sourcePackage.sources.map { it.content }
      val actual = Compiler.forStrings(generatedSrcs + S3ConnectorTaxi.schema)
         .compile()
      val expectedSrc ="""
namespace com.foo.persondata {
   type PersondataId inherits Int
   type FirstName inherits String
   type LastName inherits String
   type DateOfBirth inherits Date
   type Salary inherits Double
   type Active inherits String
   type TemperatureCelsius inherits Double
   type FavoriteColor inherits String
   type JoinedAt inherits DateTime
   type Scores inherits String
   type Notes inherits String
}
namespace com.foo {
   @com.orbitalhq.formats.Csv(delimiter = "|" , nullValue = "NULL")
   closed model PersonData {
      id : com.foo.persondata.PersondataId
      first_name : com.foo.persondata.FirstName
      last_name : com.foo.persondata.LastName
      date_of_birth : com.foo.persondata.DateOfBirth
      salary : com.foo.persondata.Salary?
      active : com.foo.persondata.Active
      temperature_celsius : com.foo.persondata.TemperatureCelsius
      favorite_color : com.foo.persondata.FavoriteColor?
      joined_at : com.foo.persondata.JoinedAt
      scores : com.foo.persondata.Scores?
      notes : com.foo.persondata.Notes?
   }

   @com.orbitalhq.aws.s3.S3Service(connectionName = "TestAwsConnection")
   service TestAwsConnectionService {
      @com.orbitalhq.aws.s3.S3Operation(bucket = "$bucketName")
      operation read(  filenamePattern : com.orbitalhq.aws.s3.FilenamePattern = "*.csv" ) : PersonData[]
   }
}
""".trimIndent()
      val expected = Compiler.forStrings(expectedSrc, S3ConnectorTaxi.schema)
         .compile()
      TestHelpers.assertAreTheSame(actual, expected, generatedSrcs)
   }


   private fun tempFile(name: String): Path {
      val csv =
         """id|first_name|last_name|date_of_birth|salary|active|temperature_celsius|favorite_color|joined_at|scores|notes
1|John|Smith|1990-01-15|75000.50|true|37.2|blue|2023-01-01T10:00:00Z|[85,92,78]|Regular customer
2|Mary|Johnson|1988-12-03|82500.75|YES|36.9|"red, blue"|2023-01-02T15:30:00+01:00|[95,88,91]|"Prefers email, not calls"
3|Bob|Williams|1995-03-21|NULL|1|38.1|green|2023-02-15T08:45:00Z|[75,80,82]|NULL
4|Alice|Brown|1992-07-30|69999|false|36.8|yellow|2023-03-20 14:30:00|NULL|First time visitor
5|Charlie|Davis|1985-11-08|92750.25|n|37.5|"black"|2023-04-01T16:15:00Z|[90,87,93]|Needs follow-up
6|Emma|Wilson|1993-04-17|78250.00|0|36.7|NULL|2023-05-10 09:00:00|[82,85,88]|"Special handling, fragile items"
7|James|Miller|1987-09-22|88750.50|TRUE|37.0|white|2023-06-01T11:20:00Z|[88,85,91]|Priority shipping
8|Sarah|Anderson|1991-06-14|71500.25|No|36.9|"purple, pink"|2023-07-15 13:45:00|[79,83,80]|"Allergic to nuts, check packages"
9|Michael|Taylor|1989-02-28|95000.00|YES|37.3|orange|2023-08-20T17:30:00+02:00|[93,91,94]|International customer
10|Emily|Thomas|1994-08-11|68250.75|F|36.8|"red"|2023-09-01 10:15:00|[87,84,86]|"Contact between 9-5 only""""
      val path = tempFolder.root.toPath().resolve(name)
      path.writeText(csv)
      return path
   }
}
