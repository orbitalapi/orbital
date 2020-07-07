package io.vyne.cask.api

import java.time.Instant

data class CaskConfig(
      val tableName: String,
      val qualifiedTypeName: String,
      val versionHash: String,
      val sourceSchemaIds: List<String>,
      val sources: List<String>,
      val deltaAgainstTableName: String?,
      val insertedAt: Instant
   )



data class CaskDetails (
   val recordsNumber: Int
)

