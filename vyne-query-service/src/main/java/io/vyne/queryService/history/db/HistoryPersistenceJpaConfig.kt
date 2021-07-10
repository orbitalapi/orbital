package io.vyne.queryService.history.db

import io.vyne.query.history.QueryResultRow
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@EnableJpaRepositories(
   basePackageClasses = [QueryResultRowRepository::class]
)
@Configuration
@EntityScan(basePackageClasses = [QueryResultRow::class])
class HistoryPersistenceJpaConfig {
}
