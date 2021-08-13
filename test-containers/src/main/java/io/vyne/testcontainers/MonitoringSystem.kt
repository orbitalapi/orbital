package io.vyne.testcontainers

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class MonitoringSystem(private val prometheus: VyneContainer, private val grafana: VyneContainer) : AutoCloseable {
   fun start() {
      prometheus.start()
      grafana.start()
      logger.warn { "Prometheus running at port => ${prometheus.firstMappedPort}" }
      logger.warn { "Grafana running at port => ${grafana.firstMappedPort}" }
   }
   override fun close() {
      prometheus.close()
      grafana.close()
   }

}

