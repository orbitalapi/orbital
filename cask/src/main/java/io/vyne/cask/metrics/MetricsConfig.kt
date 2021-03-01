package io.vyne.cask.metrics


object Meters {
   const val INGEST_FROM_KAFKA = "cask.ingest.kafka"
   const val UPSERT_PERSIST = "cask.ingest.db.upsert"
   const val INSERT_PERSIST = "cask.ingest.db.insert"
   const val PERSISTED_COUNT = "cask.ingest.count"
}
