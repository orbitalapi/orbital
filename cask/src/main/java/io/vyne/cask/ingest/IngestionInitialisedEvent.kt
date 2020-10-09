package io.vyne.cask.ingest

import io.vyne.schemas.VersionedType
import org.springframework.context.ApplicationEvent

// Raised When:
// 1. A web socket session is established for an ingestion pipeline.
// 2. A data file uploaded from UI for cask creation.
class IngestionInitialisedEvent(source: Any, val type: VersionedType): ApplicationEvent(source)
