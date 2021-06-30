package io.vyne.cask.ingest

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class IngesterFactory(val jdbcTemplate: JdbcTemplate, val caskIngestionErrorProcessor: CaskIngestionErrorProcessor, val meterRegistry: MeterRegistry) {
    fun create(ingestionStream: IngestionStream): Ingester {
        return Ingester(jdbcTemplate, ingestionStream, caskIngestionErrorProcessor.sink(), meterRegistry)
    }
}
