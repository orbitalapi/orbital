package io.vyne.testcontainers

typealias DockerImageTag = String

object CommonSettings {
   const val latest: DockerImageTag = "latest"
   const val latestSnapshot: DockerImageTag = "latest-snapshot"
   const val VyneQueryServerDefaultPort = 9022
   const val EurekaServerDefaultPort = 8761
   const val SchemaServerDefaultPort = 9305
   const val CaskDefaultPort = 8800
   const val PipelineOrchestratorDefaultPort = 9600
   const val PipelineRunnerDefaultPort = 9610
   const val PostgresPort = 5432
   const val schemaServerSchemaPath = "--taxi.schema-local-storage"
   const val eurekaServerUri = "--eureka.uri"
   const val defaultSchemaServerName = "SCHEMA-SERVER"
   const val defaultQueryServerName = "QUERY-SERVICE"
   const val defaultCaskServerName = "CASK"
   const val defaultPipelineOrchestratorName = "PIPELINES-ORCHESTRATOR"
   const val defaultPipelineRunnerApp = "PIPELINE-RUNNER"
   const val actuatorHealthEndPoint = "/api/actuator/health"
   const val pipelineRunnerActuatorHealthEndPoint = "/actuator/health"
}
