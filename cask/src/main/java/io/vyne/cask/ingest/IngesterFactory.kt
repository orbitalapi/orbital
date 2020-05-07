package io.vyne.cask.ingest

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class IngesterFactory(val jdbcTemplate: JdbcTemplate) {
    fun create(ingestionStream: IngestionStream): Ingester {
        return Ingester(jdbcTemplate, ingestionStream)
    }
}