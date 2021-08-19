package io.vyne.support

import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.rnorth.ducttape.timeouts.Timeouts
import org.rnorth.ducttape.unreliables.Unreliables
import java.io.File
import java.util.concurrent.TimeUnit

class VyneUtil(private val firstMappedPort: String) {
   /**
    * Uploads the given CSV file to Vyne System by posting
    * @param csvFilePath Full Path to the CSV file. File name should be in fullqualifiedtypename.csv
    * @return Http Response Code.
    */
   fun postCsvDataFile(csvFilePath: String): Int {
      val csvFile = File(csvFilePath)
      return this.postCsvDataFile(csvFile)
   }

   /**
    * Uploads the given CSV file to Vyne System by posting
    * @param csvFile CSV file. File name should be in fullqualifiedtypename.csv
    * @return Http Response Code.
    */
   fun postCsvDataFile(csvFile: File): Int {
      val typeName = csvFile.nameWithoutExtension
      return postCsvDataFile(csvFile, typeName)
   }

   /**
    * Uploads the given CSV file to Vyne System by posting
    * @param csvFile CSV file.
    * @param fullyQualifiedTypeName Fully Qualified Type Name for the csv data.
    * @return Http Response Code.
    */
   fun postCsvDataFile(csvFile: File, fullyQualifiedTypeName: String): Int {
      val exposedPort = this.firstMappedPort
      val request = Request
         .post("http://localhost:$exposedPort/api/ingest/csv/$fullyQualifiedTypeName?delimiter=,&firstRecordAsHeader=true&containsTrailingDelimiters=false")
         .bodyFile(csvFile, ContentType.MULTIPART_FORM_DATA)
         .setHeader("Accept", "application/json, text/plain, */*")
         .setHeader("Content-Type", "text/plain")

      return request.execute().returnResponse().code
   }

   /**
    * Uploads the given JSON file to Vyne System by posting
    * @param jsonFile Json file.
    * @param fullyQualifiedTypeName Fully Qualified Type Name for the csv data.
    * @return Http Response Code.
    */
   fun postJsonDataFile(jsonFile: File, fullyQualifiedTypeName: String): Int {
      val exposedPort = this.firstMappedPort
      val request = Request
         .post("http://localhost:$exposedPort/api/ingest/json/$fullyQualifiedTypeName")
         .bodyFile(jsonFile, ContentType.MULTIPART_FORM_DATA)
         .setHeader("Accept", "application/json, text/plain, */*")
         .setHeader("Content-Type", "application/json")

      return request.execute().returnResponse().code
   }

   fun postXmlDataFile(xmlFile: File, fullyQualifiedTypeName: String, elementSelector: String? = null): Int {
      val uri = "http://localhost:${this.firstMappedPort}/api/ingest/xml/${fullyQualifiedTypeName}"
      val postUri =  if (elementSelector != null) "$uri?elementSelector=$elementSelector" else uri
      val request = Request
         .post(postUri)
         .bodyFile(xmlFile, ContentType.MULTIPART_FORM_DATA)
         .setHeader("Accept", "application/json, text/plain, */*")
         .setHeader("Content-Type", "text/plain")

      return request.execute().returnResponse().code

   }

   /**
    * Submits the given Vyne Query against VyneQL Query Point.
    * @param vyneQlQuery VyneQL Query.
    * @return Query Result in csv format.
    */
   fun submitVyneQl(vyneQlQuery: String): Int {
      val exposedPort = this.firstMappedPort
      val request = Request
         .post("http://localhost:$exposedPort/api/vyneql?resultMode=RAW")
         .bodyString(vyneQlQuery, ContentType.APPLICATION_JSON)
         .setHeader("Accept", "text/csv")
         .setHeader("Content-Type", "application/json")
      return request.execute().returnResponse().code
   }

   fun fetchSchemaSources(): String {
      return Request
         .get("http://localhost:${this.firstMappedPort}/api/schemas")
         .setHeader("Content-Type", "application/json")
         .execute()
         .returnContent().asString()
   }

   fun deleteCask(fullyQualifiedTypeName: String): Int {
      return Request
         .delete("http://localhost:${this.firstMappedPort}/api/types/cask/$fullyQualifiedTypeName?force=true")
         .setHeader("Content-Type", "application/json")
         .execute().returnResponse().code
   }

   fun ensureService(fullyQualifiedServiceName: String,
                     retryCountLimit: Int = 5,
                     waitInMillisecondsBetweenRetries: Long = 30000L) {
      Unreliables.retryUntilSuccess(retryCountLimit) {
         Timeouts.doWithTimeout(1, TimeUnit.MINUTES) {
            val response = Request
               .get("http://localhost:${this.firstMappedPort}/api/services/$fullyQualifiedServiceName")
               .setHeader("Content-Type", "application/json")
               .execute()
            if (response.returnResponse().code == 500) {
               Thread.sleep(waitInMillisecondsBetweenRetries)
               throw IllegalStateException("$fullyQualifiedServiceName service can't be found!")
            }
         }
      }
   }

   fun ensureType(fullyQualifiedTypeName: String, retryCountLimit: Int = 5, waitInMillisecondsBetweenRetries: Long = 30000L) {
      Unreliables.retryUntilSuccess(retryCountLimit) {
         Timeouts.doWithTimeout(1, TimeUnit.MINUTES) {
            val response = Request
               .get("http://localhost:${this.firstMappedPort}/api/types/$fullyQualifiedTypeName")
               .setHeader("Content-Type", "application/json")
               .execute()
            if (response.returnResponse().code != 200) {
               Thread.sleep(waitInMillisecondsBetweenRetries)
               throw IllegalStateException("$fullyQualifiedTypeName service can't be found!")
            }
         }
      }
   }

}
