package com.orbitalhq.schema.publisher.spring

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the Schema Publisher Spring Boot Starter.
 *
 * Configures how Taxi schemas are published from a Spring Boot application.
 */
@ConfigurationProperties(prefix = "orbital.schema.publisher")
data class SchemaPublisherProperties(
   /**
     * Whether schema publishing is enabled.
     * Defaults to true.
     */
    val enabled: Boolean = true,

   /**
     * The unique identifier for this publisher.
     * Defaults to ${spring.application.name} if not specified.
     */
    val publisherId: String? = null,

   /**
     * The URL of the schema server.
     * Defaults to http://localhost:9022
     */
    val orbitalUrl: String = "http://localhost:9022",

   /**
     * Connection timeout in milliseconds.
     * Defaults to 5000ms (5 seconds).
     */
    val connectionTimeout: Long = 5000,

   /**
     * The id for the project when published. Should be in the form of organisation/projectName/version (eg., com.acme/foo/1.0.0)
     * If not provided, one is assumed by using the package of the application class
     *
     */
   val packageId: String? = null,

   val baseUrl: String? = null
)
