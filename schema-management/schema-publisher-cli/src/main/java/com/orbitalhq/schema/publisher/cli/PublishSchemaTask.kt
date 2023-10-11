package com.orbitalhq.schema.publisher.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schema.publisher.SchemaPublisherService
import com.orbitalhq.schema.publisher.SourceSubmissionResponse
import com.orbitalhq.schema.publisher.http.HttpSchemaPublisher
import com.orbitalhq.schema.publisher.http.HttpSchemaSubmitter
import com.orbitalhq.toPackageMetadata
import kotlinx.coroutines.runBlocking
import lang.taxi.generators.openApi.TaxiGenerator
import lang.taxi.packages.TaxiPackageLoader
import org.apache.commons.io.FilenameUtils
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.readText

class PublishSchemaTask(val specPath: Path, val serverUri: URI, val taxiConfPath: Path) {
   fun run() {
      // Convert the swagger to Taxi
      val spec = specPath.readText()
      val taxiConf = TaxiPackageLoader().withConfigFileAt(taxiConfPath).load()
      val generatedTaxi = TaxiGenerator().generateAsStrings(spec, taxiConf.identifier.name.organisation)

      val publisher = SchemaPublisherService(
         "cli",
         HttpSchemaPublisher(
            SimpleHttpPublisher(serverUri),
            Duration.ofSeconds(5)
         )
      )
      publisher.publish(
         SourcePackage(
            taxiConf.toPackageMetadata(),
            listOf(
               VersionedSource(
                  specPath.fileName.toString(),
                  taxiConf.version,
                  generatedTaxi.concatenatedSource
               )
            ),
            emptyMap()
         )
      ).subscribe()
   }
}


class SimpleHttpPublisher(val uri: URI) : HttpSchemaSubmitter {
   val client = HttpClient(CIO) {
      install(ContentNegotiation) {
         jackson() {
            this.findAndRegisterModules()
         }
      }
   }
   override fun submitSources(submission: SourcePackage): Mono<SourceSubmissionResponse> {
      return runBlocking {
         val urlString = uri.resolve(URI.create("/api/schemas/taxi")).toURL()
         val response = client.post(urlString) {

            contentType(ContentType.Application.Json)
            setBody(submission)
         }
         val submissionResponse:SourceSubmissionResponse = response.body()
         Mono.just(submissionResponse)
      }
   }

}
