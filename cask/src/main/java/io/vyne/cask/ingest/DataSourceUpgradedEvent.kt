package io.vyne.cask.ingest

import org.springframework.context.ApplicationEvent

class DataSourceUpgradedEvent(source: Any, val strategy: UpgradeDataSourceSpec): ApplicationEvent(source)
