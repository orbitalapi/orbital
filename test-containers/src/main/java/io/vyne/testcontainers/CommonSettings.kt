package io.vyne.testcontainers

typealias DockerImageTag = String

object CommonSettings {
   const val latest: DockerImageTag = "latest"
   const val latestSnapshot: DockerImageTag = "latest-snapshot"
   const val VyneQueryServerDefaultPort = 9022
   const val EurekaServerDefaultPort = 8761
   const val FileSchemaServerDefaultPort = 9301
   const val CaskDefaultPort = 8800
   const val fileSchemaServerSchemaPath = "--taxi.schema-local-storage"
   const val eurekaServerUri = "--eureka.uri"
   const val defaultFileSchemaServerName = "FILE-SCHEMA-SERVER"
   const val defaultQueryServerName = "QUERY-SERVICE"
   const val defaultCaskServerName = "CASK"
   const val actuatorHealthEndPoint = "/actuator/health"
}
