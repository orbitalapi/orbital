package io.vyne.cask.ingest

import io.vyne.schemas.VersionedType
import org.springframework.context.ApplicationEvent

class IngestionInitialisedEvent(source: Any, val type: VersionedType): ApplicationEvent(source)
