package com.orbitalhq.schema.publisher.spring

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.schema.publisher.SchemaPublisherService
import jakarta.annotation.PostConstruct
import lang.taxi.generators.java.spring.SpringTaxiGenerator
import mu.KotlinLogging

/**
 * SchemaLoader is responsible for discovering and loading Taxi schemas
 * from the Spring Boot application and publishing them to the schema server.
 *
 * This component:
 * - Discovers Taxi schemas in the application (from annotations, files, etc.)
 * - Loads and parses the schemas
 * - Publishes them to the configured schema server on application startup
 */
class SchemaPublisherTask(
   private val publisherService: SchemaPublisherService,
   private val properties: SchemaPublisherProperties,
   private val packageNamesToScan: List<String>,
) {

   private val logger = KotlinLogging.logger {}

   /**
    * Initializes the schema loader and publishes schemas on application startup.
    */
   @PostConstruct
   fun initialize() {
      val packageId = if (properties.packageId.isNullOrEmpty()) {
         val packageIdentifier = PackageIdentifier(packageNamesToScan[0], publisherService.publisherId, "0.0.0")
         logger.warn { "A package identifier was not provided. Using ${packageIdentifier.id} as a default, but you should consider setting --orbital.schema.publisher.package-id to a value in the form of org/projectName/version" }
         packageIdentifier
      } else {
         PackageIdentifier.fromId(properties.packageId)
      }
      if (properties.baseUrl == null) {
         logger.warn { "No baseUrl was provided. Orbital will infer one based on the inbound connection when the schema is published, but you should consider setting --orbital.schema.publisher.base-url" }
      }
      logger.info { "Starting schema publication for publisher id: ${publisherService.publisherId} with packageId: ${packageId.id}" }
      publisherService.publish(
         PackageMetadata.from(packageId),
         SpringTaxiGenerator.forBaseUrl(properties.baseUrl.orEmpty())
            .forPackageNames(packageNamesToScan)
            .generate()
      ).subscribe()
   }

}
